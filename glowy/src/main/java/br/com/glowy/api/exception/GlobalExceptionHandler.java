package br.com.glowy.api.exception;

import br.com.glowy.api.dto.ErroResponseDTO;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErroResponseDTO> montar(HttpStatus status, String erro, String mensagem,
                                                   HttpServletRequest request, List<String> detalhes) {
        ErroResponseDTO corpo = new ErroResponseDTO(
                LocalDateTime.now(), status.value(), erro, mensagem, request.getRequestURI(), detalhes);
        return ResponseEntity.status(status).body(corpo);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErroResponseDTO> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), request, List.of());
    }

    // Corpo ilegível, parâmetro com tipo errado (ex.: status inexistente) ou cabeçalho/parâmetro obrigatório ausente
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ErroResponseDTO> handleMalformed(Exception ex, HttpServletRequest request) {
        return montar(HttpStatus.BAD_REQUEST, "Requisição malformada",
                "Corpo, parâmetro ou cabeçalho ausente ou em formato inválido.", request, List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErroResponseDTO> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErroResponseDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroResponseDTO> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Erro de validação nos dados enviados",
                "Verifique os campos inválidos na requisição.", request, detalhes);
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ErroResponseDTO> handleConflict(Exception ex, HttpServletRequest request) {
        String mensagem = (ex instanceof ObjectOptimisticLockingFailureException || ex instanceof OptimisticLockException)
                ? "Conflito de concorrência ao atualizar o estoque do produto. Tente novamente."
                : "Registro duplicado ou vinculado a outro recurso ativo.";
        return montar(HttpStatus.CONFLICT, "Conflito de dados", mensagem, request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleGeneral(Exception ex, HttpServletRequest request) {
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", ex.getMessage(), request, List.of());
    }
}
