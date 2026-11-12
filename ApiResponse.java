package cn.edu.sdua.geren.common;

/**
 * 统一响应结构：code=0 表示成功，code=1 表示业务失败。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(1, message, null);
    }
}
