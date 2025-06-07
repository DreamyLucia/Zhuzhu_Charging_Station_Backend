package org.zhuzhu_charging_station_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.zhuzhu_charging_station_backend.dto.StandardResponse;
import io.jsonwebtoken.JwtException;

/**
 * 全局异常处理器，统一格式化所有Controller抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务非法操作，返回400
     */
    @ExceptionHandler(BadStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public StandardResponse<?> handleBadOrderStateException(BadStateException e) {
        return StandardResponse.error(400, e.getMessage());
    }

    /**
     * 认证失败（如登录用户名/密码错误），返回401
     */
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public StandardResponse<?> handleAuthException(RuntimeException e) {
        return StandardResponse.error(401, e.getMessage());
    }

    /**
     * token 非法/失效时，返回401
     */
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public StandardResponse<?> handleJwtException(JwtException e) {
        return StandardResponse.error(401, "令牌无效或已过期");
    }

    /**
     * 业务无权限异常（用于鉴权失败时主动抛出），返回403
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public StandardResponse<?> handleForbiddenException(ForbiddenException e) {
        return StandardResponse.error(403, e.getMessage());
    }

    /**
     * 捕获查无/删无等资源不存在异常，返回404
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public StandardResponse<?> handleNotFoundException(NotFoundException e) {
        return StandardResponse.error(404, e.getMessage());
    }

    /**
     * 资源已存在异常，新增/保存唯一约束冲突时抛出，返回409
     */
    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public StandardResponse<?> handleAlreadyExistsException(AlreadyExistsException e) {
        return StandardResponse.error(409, e.getMessage());
    }

    /**
     * 兜底处理其他异常，返回500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public StandardResponse<?> handleException(Exception e) {
        return StandardResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}