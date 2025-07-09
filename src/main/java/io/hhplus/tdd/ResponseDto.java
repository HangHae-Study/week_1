package io.hhplus.tdd;


public class ResponseDto<T> {

    private String code;
    private String message;
    private T data;

    public ResponseDto(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseDto<T> success(T data) {
        return new ResponseDto<>("SUCCESS", "정상 처리되었습니다.", data);
    }

    public static <T> ResponseDto<T> error(String message) {
        return new ResponseDto<>("ERROR", message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}