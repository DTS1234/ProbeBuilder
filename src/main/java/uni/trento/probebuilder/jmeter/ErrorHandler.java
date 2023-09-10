package uni.trento.probebuilder.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleErrors(IllegalStateException e) {
        log.error("ERROR occurred! : " + e.getMessage());
        return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
    }

}
