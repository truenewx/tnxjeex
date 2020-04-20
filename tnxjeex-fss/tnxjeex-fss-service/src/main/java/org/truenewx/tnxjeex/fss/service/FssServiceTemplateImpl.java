package org.truenewx.tnxjeex.fss.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.truenewx.tnxjee.core.Strings;
import org.truenewx.tnxjee.core.beans.ContextInitializedBean;
import org.truenewx.tnxjee.core.util.EncryptUtil;
import org.truenewx.tnxjee.model.spec.user.UserIdentity;
import org.truenewx.tnxjee.service.exception.BusinessException;
import org.truenewx.tnxjeex.fss.service.model.*;

/**
 * 文件存储服务模版实现
 *
 * @author jianglei
 */
public class FssServiceTemplateImpl<T extends Enum<T>, I extends UserIdentity>
        implements FssServiceTemplate<T, I>, ContextInitializedBean {

    private final Map<T, FssAccessStrategy<T, I>> strategies = new HashMap<>();
    private final Map<FssProvider, FssAuthorizer> authorizers = new HashMap<>();
    private final Map<FssProvider, FssProviderAccessor> providerAccessors = new HashMap<>();
    private FssLocalAccessor localAccessor;

    @Autowired
    public void setLocalAccessor(FssLocalAccessor localAccessor) {
        this.localAccessor = localAccessor;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void afterInitialized(ApplicationContext context) throws Exception {
        Map<String, FssAccessStrategy> strategies = context.getBeansOfType(FssAccessStrategy.class);
        for (FssAccessStrategy<T, I> strategy : strategies.values()) {
            this.strategies.put(strategy.getType(), strategy);
        }

        Map<String, FssAuthorizer> authorizers = context.getBeansOfType(FssAuthorizer.class);
        for (FssAuthorizer authorizer : authorizers.values()) {
            this.authorizers.put(authorizer.getProvider(), authorizer);
        }

        Map<String, FssProviderAccessor> accessors = context.getBeansOfType(FssProviderAccessor.class);
        for (FssProviderAccessor accessor : accessors.values()) {
            this.providerAccessors.put(accessor.getProvider(), accessor);
        }
    }

    @Override
    public FssUploadLimit getUploadLimit(T type, I userIdentity) {
        return getStrategy(type).getUploadLimit(userIdentity);
    }

    private FssAccessStrategy<T, I> getStrategy(T type) {
        FssAccessStrategy<T, I> strategy = this.strategies.get(type);
        if (strategy == null) {
            throw new BusinessException(FssExceptionCodes.NO_ACCESS_STRATEGY_FOR_TYPE,
                    type.name());
        }
        return strategy;
    }

    @Override
    public String write(T type, String resource, I userIdentity, String filename, InputStream in)
            throws IOException {
        FssAccessStrategy<T, I> policy = getStrategy(type);
        String extension = validateExtension(policy, userIdentity, filename);
        FssProvider provider = policy.getProvider();
        // 用BufferedInputStream装载以确保输入流可以标记和重置位置
        in = new BufferedInputStream(in);
        in.mark(Integer.MAX_VALUE);
        String path;
        if (policy.isMd5AsFilename()) {
            String md5Code = EncryptUtil.encryptByMd5(in);
            in.reset();
            path = policy.getPath(resource, userIdentity, md5Code + extension);
        } else {
            path = policy.getPath(resource, userIdentity, filename);
        }
        if (path == null) {
            throw new BusinessException(FssExceptionCodes.NO_WRITE_AUTHORITY);
        }
        path = standardizePath(path);
        if (!policy.isWritable(userIdentity, path)) {
            throw new BusinessException(FssExceptionCodes.NO_WRITE_AUTHORITY);
        }

        String bucket = policy.getBucket();
        // 如果方针指定需要本地存储，则进行本地存储；
        // 但如果此时服务提供商是自有，则为了避免重复存储，跳过本地存储
        if (policy.isStoreLocally() && provider != FssProvider.OWN) {
            this.localAccessor.write(bucket, path, filename, in);
        }
        FssProviderAccessor providerAccessor = this.providerAccessors.get(provider);
        if (providerAccessor != null) {
            in.reset(); // 读取输入流之前先重置，以重新读取
            providerAccessor.write(bucket, path, filename, in);
        }
        if (policy.isPublicReadable()) {
            FssAuthorizer authorizer = this.authorizers.get(provider);
            authorizer.authorizePublicRead(bucket, path);
        }
        return getStorageUrl(provider, bucket, path);
    }

    private String validateExtension(FssAccessStrategy<T, I> policy, I user,
            String filename) {
        String extension = FilenameUtils.getExtension(filename);
        FssUploadLimit uploadLimit = policy.getUploadLimit(user);
        String[] extensions = uploadLimit.getExtensions();
        if (ArrayUtils.isNotEmpty(extensions)) { // 上传限制中没有设置扩展名，则不限定扩展名
            if (uploadLimit.isRejectedExtension()) { // 拒绝扩展名模式
                if (ArrayUtils.contains(extensions, extension)) {
                    throw new BusinessException(FssExceptionCodes.UNSUPPORTED_EXTENSION,
                            StringUtils.join(extensions, Strings.COMMA), filename);
                }
            } else { // 允许扩展名模式
                if (!ArrayUtils.contains(extensions, extension)) {
                    throw new BusinessException(FssExceptionCodes.ONLY_SUPPORTED_EXTENSION,
                            StringUtils.join(extensions, Strings.COMMA), filename);
                }
            }
        }
        if (extension.length() > 0) {
            extension = Strings.DOT + extension;
        }
        return extension;
    }

    protected String getStorageUrl(FssProvider provider, String bucket, String path) {
        return new FssStorageUrl(provider, bucket, path).toString();
    }

    /**
     * 使路径格式标准化，不以斜杠开头，也不以斜杠结尾<br/>
     * 所有存储服务提供商均接收该标准的路径，如服务提供商对路径的要求与此不同，则服务提供商的实现类中再做转换
     *
     * @param path 标准化前的路径
     * @return 标准化后的路径
     */
    private String standardizePath(String path) {
        if (path.startsWith(Strings.SLASH)) { // 不能以斜杠开头
            return path.substring(1);
        }
        if (path.endsWith(Strings.SLASH)) { // 不能以斜杠结尾
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Override
    public String getReadUrl(I userIdentity, String storageUrl, boolean thumbnail) {
        return getReadUrl(userIdentity, new FssStorageUrl(storageUrl), thumbnail);
    }

    private String getReadUrl(I userIdentity, FssStorageUrl url, boolean thumbnail) {
        if (url.isValid()) {
            String bucket = url.getBucket();
            String path = standardizePath(url.getPath());
            FssAccessStrategy<T, I> policy = validateUserRead(userIdentity, bucket, path);
            // 如果方针要求读取地址为本地地址，则使用自有提供商
            FssProvider provider = policy.isReadLocally() ? FssProvider.OWN
                    : url.getProvider(); // 使用内部协议确定的提供商而不是方针下现有的提供商，以免方针的历史提供商有变化
            FssAuthorizer authorizer = this.authorizers.get(provider);
            if (thumbnail) {
                path = appendThumbnailParameters(policy, path);
            }
            return authorizer.getReadUrl(userIdentity, bucket, path);
        }
        return null;
    }

    private String appendThumbnailParameters(FssAccessStrategy<T, I> policy, String path) {
        if (policy != null) {
            Map<String, String> thumbnailParameters = policy.getThumbnailParameters();
            if (thumbnailParameters != null && thumbnailParameters.size() > 0) {
                StringBuilder params = new StringBuilder();
                for (Entry<String, String> entry : thumbnailParameters.entrySet()) {
                    params.append(Strings.AND).append(entry.getKey()).append(Strings.EQUAL)
                            .append(entry.getValue());
                }
                if (params.length() > 0) {
                    params.deleteCharAt(0);
                }
                int index = path.indexOf(Strings.QUESTION);
                // 确保缩略参数作为优先参数
                if (index > 0) {
                    path = path.substring(0, index + 1) + params + Strings.AND
                            + path.substring(index + 1);
                } else {
                    path += Strings.QUESTION + params;
                }
            }
        }
        return path;
    }

    private FssAccessStrategy<T, I> validateUserRead(I user, String bucket, String path) {
        // 存储桶相同，且用户对指定路径具有读权限，则匹配
        // 这要求方针具有唯一的存储桶，或者与其它方针的存储桶相同时，下级存放路径不同
        FssAccessStrategy<T, I> policy = this.strategies.values().stream()
                .filter(p -> p.getBucket().equals(bucket) && p.isReadable(user, path)).findFirst()
                .orElse(null);
        if (policy == null) {
            // 如果没有找到匹配的方针，则说明没有读权限
            String url = Strings.SLASH + bucket + path;
            throw new BusinessException(FssExceptionCodes.NO_READ_AUTHORITY, url);
        }
        return policy;
    }

    @Override
    public FssReadMetadata getReadMetadata(I userIdentity, String storageUrl) {
        FssStorageUrl url = new FssStorageUrl(storageUrl);
        String readUrl = getReadUrl(userIdentity, url, false);
        if (readUrl != null) { // 不为null，则说明存储url有效且用户权限校验通过
            // 先尝试从本地获取
            FssStorageMetadata storageMetadata = this.localAccessor
                    .getStorageMetadata(url.getBucket(), url.getPath());
            if (storageMetadata == null) {
                // 本地无法获取才尝试从服务提供商处获取
                FssProvider provider = url.getProvider();
                FssProviderAccessor providerAccessor = this.providerAccessors.get(provider);
                if (providerAccessor != null) {
                    storageMetadata = providerAccessor.getStorageMetadata(url.getBucket(),
                            url.getPath());
                }
            }
            if (storageMetadata != null) {
                String thumbnailReadUrl = getReadUrl(userIdentity, url, true);
                return new FssReadMetadata(readUrl, thumbnailReadUrl, storageMetadata);
            }
        }
        return null;
    }

    @Override
    public long getLastModifiedTime(I userIdentity, String bucket, String path) {
        path = standardizePath(path);
        FssAccessStrategy<T, I> policy = validateUserRead(userIdentity, bucket, path);
        long lastModifiedTime = this.localAccessor.getLastModifiedTime(bucket, path);
        if (lastModifiedTime <= 0) {
            FssProviderAccessor providerAccessor = this.providerAccessors
                    .get(policy.getProvider());
            if (providerAccessor != null) {
                lastModifiedTime = providerAccessor.getLastModifiedTime(bucket, path);
            }
        }
        return lastModifiedTime;
    }

    @Override
    public void read(I userIdentity, String bucket, String path, OutputStream out)
            throws IOException {
        path = standardizePath(path);
        FssAccessStrategy<T, I> policy = validateUserRead(userIdentity, bucket, path); // 校验读取权限
        if (!this.localAccessor.read(bucket, path, out)) {
            FssProviderAccessor providerAccessor = this.providerAccessors
                    .get(policy.getProvider());
            if (providerAccessor != null) {
                providerAccessor.read(bucket, path, out);
            }
        }
    }

}
