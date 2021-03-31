package org.truenewx.tnxjeex.office.excel;

/**
 * Excel导入异常错误码集
 *
 * @author jianglei
 */
public class ExcelExceptionCodes {

    private ExcelExceptionCodes() {
    }

    /**
     * 单元格字符串格式错误
     */
    public static final String CELL_STRING_FORMAT_ERROR = "tnxjeex.office.excel.cell.string_format_error";

    /**
     * 单元格数字格式错误
     */
    public static final String CELL_NUMBER_FORMAT_ERROR = "tnxjeex.office.excel.cell.number_format_error";

    /**
     * 单元格日期格式错误
     */
    public static final String CELL_DATE_FORMAT_ERROR = "tnxjeex.office.excel.cell.date_format_error";

    /**
     * 单元格月份格式错误
     */
    public static final String CELL_MONTH_FORMAT_ERROR = "tnxjeex.office.excel.cell.month_format_error";

    /**
     * 导入：单元格必填
     */
    public static final String IMPORT_CELL_REQUIRED = "tnxjeex.office.excel.import.cell_required";


}
