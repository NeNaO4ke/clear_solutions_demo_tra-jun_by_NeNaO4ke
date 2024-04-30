package demo.clear_solutions_demo_trajun.configuration;

import demo.clear_solutions_demo_trajun.exception.ApiErrorDetail;
import demo.clear_solutions_demo_trajun.exception.ApiFieldErrorDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {
    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(
            WebExchangeBindException ex, HttpHeaders headers, HttpStatusCode status,
            ServerWebExchange exchange) {
        List<ApiFieldErrorDetail> errorList = ex.getBindingResult().getFieldErrors()
                .stream().map(fe -> new ApiFieldErrorDetail(fe.getField(), fe.getObjectName(), fe.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(ex, new ApiErrorDetail(ex.getBody(), errorList), headers, status, exchange);
    }
}
