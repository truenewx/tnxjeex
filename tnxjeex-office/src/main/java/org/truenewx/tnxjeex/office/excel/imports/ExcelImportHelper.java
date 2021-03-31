package org.truenewx.tnxjeex.office.excel.imports;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.truenewx.tnxjee.core.enums.EnumDictResolver;
import org.truenewx.tnxjee.core.enums.EnumItem;
import org.truenewx.tnxjee.core.enums.EnumItemKey;
import org.truenewx.tnxjee.core.enums.EnumType;
import org.truenewx.tnxjee.core.util.BeanUtil;
import org.truenewx.tnxjee.core.util.ClassUtil;
import org.truenewx.tnxjee.core.util.MathUtil;
import org.truenewx.tnxjee.model.validation.constraint.RegionCode;
import org.truenewx.tnxjee.service.exception.BusinessException;
import org.truenewx.tnxjee.service.exception.message.CodedErrorResolver;
import org.truenewx.tnxjee.service.exception.model.CodedError;
import org.truenewx.tnxjee.service.spec.region.NationalRegionSource;
import org.truenewx.tnxjee.service.spec.region.Region;
import org.truenewx.tnxjee.service.spec.region.RegionNationCodes;
import org.truenewx.tnxjee.service.spec.region.RegionSource;
import org.truenewx.tnxjeex.office.excel.ExcelExceptionCodes;
import org.truenewx.tnxjeex.office.excel.ExcelRow;

/**
 * Excel导入协助者
 *
 * @author jianglei
 */
@Component
public class ExcelImportHelper {

    @Autowired
    private CodedErrorResolver codedErrorResolver;
    @Autowired
    private EnumDictResolver enumDictResolver;
    @Autowired
    private RegionSource regionSource;

    public void addSheetError(ImportingExcelSheetModel<?> sheetModel, String code, Locale locale, Object... args) {
        CodedError error = this.codedErrorResolver.resolveError(code, locale, args);
        sheetModel.getErrors().add(error);
    }

    public void addRowError(ImportingExcelRowModel rowModel, String code, Locale locale, Object... args) {
        CodedError error = this.codedErrorResolver.resolveError(code, locale, args);
        rowModel.getRowErrors().add(error);
    }

    public void addCellError(ImportingExcelRowModel rowModel, String fieldName, Object fieldValue, String errorCode,
            Locale locale, Object... args) {
        CodedError error = this.codedErrorResolver.resolveError(errorCode, locale, args);
        rowModel.addFieldError(fieldName, fieldValue == null ? null : fieldValue.toString(), error);
    }

    public void addCellError(ImportingExcelRowModel rowModel, String fieldName, int index, Object fieldValue,
            String errorCode, Locale locale, Object... args) {
        CodedError error = this.codedErrorResolver.resolveError(errorCode, locale, args);
        rowModel.addFieldError(fieldName, index, fieldValue == null ? null : fieldValue.toString(), error);
    }

    public void addCellError(ImportingExcelRowModel rowModel, String fieldName, Object fieldValue, BusinessException be,
            Locale locale) {
        addCellError(rowModel, fieldName, fieldValue, be.getCode(), locale, be.getArgs());
    }

    public void addCellRequiredError(ImportingExcelRowModel rowModel, String fieldName, Locale locale) {
        addCellError(rowModel, fieldName, null, ExcelExceptionCodes.IMPORT_CELL_REQUIRED, locale);
    }

    public void applyValue(ImportingExcelRowModel rowModel, ExcelRow row, int columnIndex, String fieldName,
            Locale locale, boolean required) {
        Object value = getCellValue(rowModel, row, columnIndex, fieldName, locale);
        applyValue(rowModel, value, fieldName, locale, required);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <V> V getCellValue(ImportingExcelRowModel rowModel, ExcelRow row, int columnIndex, String fieldName,
            Locale locale) {
        Field field = ClassUtil.findField(rowModel.getClass(), fieldName);
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            String value = row.getStringCellValue(columnIndex);
            EnumItemKey enumItemKey = field.getAnnotation(EnumItemKey.class);
            if (enumItemKey != null) {
                EnumType enumType = this.enumDictResolver
                        .getEnumType(enumItemKey.type(), enumItemKey.subtype(), locale);
                if (enumType != null) {
                    EnumItem enumItem = enumType.getItemByCaption(value);
                    if (enumItem != null) {
                        return (V) enumItem.getKey();
                    }
                }
                addCellError(rowModel, fieldName, value, ExcelExceptionCodes.IMPORT_CELL_ENUM_ERROR, locale);
            }
            RegionCode regionCode = field.getAnnotation(RegionCode.class);
            if (regionCode != null) {
                Region region = getNationalRegionSource().parseSubRegion(value, regionCode.withSuffix(), locale);
                if (region != null) {
                    return (V) region.getCode();
                }
                addCellError(rowModel, fieldName, value, ExcelExceptionCodes.IMPORT_CELL_REGION_ERROR, locale);
            }
            return (V) value;
        } else if (fieldType.isEnum()) {
            String caption = row.getStringCellValue(columnIndex);
            if (StringUtils.isNotBlank(caption)) {
                Class<Enum> enumClass = (Class<Enum>) fieldType;
                V value = (V) this.enumDictResolver.getEnumConstantByCaption(enumClass, caption, locale);
                if (value == null) {
                    addCellError(rowModel, fieldName, caption, ExcelExceptionCodes.IMPORT_CELL_ENUM_ERROR, locale);
                }
                return value;
            }
            return null;
        } else if (fieldType == LocalDate.class) {
            return (V) row.getLocalDateCellValue(columnIndex);
        } else {
            BigDecimal decimal = row.getNumericCellValue(columnIndex);
            return (V) MathUtil.toValue(decimal, fieldType);
        }
    }

    public NationalRegionSource getNationalRegionSource() {
        return this.regionSource.getNationalRegionSource(RegionNationCodes.CHINA);
    }

    public void applyValue(ImportingExcelRowModel rowModel, Object fieldValue, String fieldName, Locale locale,
            boolean required) {
        if (required) {
            if (fieldValue == null || (fieldValue instanceof String && StringUtils.isBlank((String) fieldValue))) {
                addCellRequiredError(rowModel, fieldName, locale);
                return;
            }
        }
        BeanUtil.setPropertyValue(rowModel, fieldName, fieldValue);
    }


}
