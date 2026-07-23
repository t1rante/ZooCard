package br.unesp.zoocard.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleRegraNegocioDevolveMensagemDaExcecao() {
        ResponseEntity<Map<String, String>> resposta =
            handler.handleRegraNegocio(new RegraNegocioException("mensagem de negocio"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("message", "mensagem de negocio");
    }

    @Test
    void handleMaxUploadSizeExceededDevolve400ComMensagemAmigavel() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10L * 1024 * 1024);

        ResponseEntity<Map<String, String>> resposta = handler.handleMaxUploadSizeExceeded(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody())
            .containsEntry("message", "A imagem excede o tamanho máximo de 10 MB.");
    }

    @Test
    void handleMethodArgumentNotValidDevolveMensagemDoPrimeiroCampoInvalido() throws Exception {
        MethodParameter parameter = parametroDoMetodoFalso();
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objeto");
        bindingResult.addError(new FieldError("objeto", "campo", "campo nao pode ser vazio"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> resposta = handler.handleMethodArgumentNotValid(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("message", "campo nao pode ser vazio");
    }

    @Test
    void handleMethodArgumentNotValidUsaMensagemGenericaQuandoNaoHaFieldError() throws Exception {
        MethodParameter parameter = parametroDoMetodoFalso();
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "objeto");

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> resposta = handler.handleMethodArgumentNotValid(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("message", "Dados inválidos.");
    }

    private static MethodParameter parametroDoMetodoFalso() throws NoSuchMethodException {
        Method metodo = ApiExceptionHandlerTest.class.getDeclaredMethod("metodoFalsoParaTeste", String.class);
        return new MethodParameter(metodo, 0);
    }

    @SuppressWarnings("unused")
    private static void metodoFalsoParaTeste(String argumento) {
    }
}
