package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.service.AuthService;
import br.com.servicehub.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web (@WebMvcTest): so controller + @RestControllerAdvice, com os services mockados.
 * Filtros de seguranca desligados aqui; 401/403 sao cobertos nos testes de integracao.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;
    @MockBean UserService userService;

    @Test
    @DisplayName("Contrato: cadastro válido responde 201 com Location e sem senha no corpo")
    void validRegisterReturns201WithLocation() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(7L, "Maria Silva", "maria.silva@email.com",
                Role.PROVIDER, null, null, null, true, null, null));

        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Maria Silva","email":"maria.silva@email.com","password":"Senha123","role":"PROVIDER"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/ServiceHub/v1/users/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Validação: campos inválidos respondem 400 em ProblemDetail com a lista errors")
    void invalidBodyReturnsProblemDetailWithErrors() throws Exception {
        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"M","email":"nao-e-email","password":"fraca","role":"CLIENT"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/ServiceHub/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());
    }

    @Test
    @DisplayName("Validação: senha sem maiúscula ou sem número é rejeitada (regra do Mapa)")
    void passwordRuleIsEnforced() throws Exception {
        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Maria Silva","email":"maria@email.com","password":"semmaiuscula1","role":"CLIENT"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());

        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Maria Silva","email":"maria@email.com","password":"SemNumeroAqui","role":"CLIENT"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());
    }

    @Test
    @DisplayName("Validação: JSON malformado responde 400 em ProblemDetail")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"));
    }

    @Test
    @DisplayName("Validação: enum desconhecido responde 400 e o detail lista os valores aceitos")
    void unknownEnumListsAcceptedValues() throws Exception {
        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Maria Silva","email":"maria@email.com","password":"Senha123","role":"SUPERUSER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("CLIENT")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("PROVIDER")));
    }

    @Test
    @DisplayName("Mass assignment (OWASP API3): campos extras como id e active são ignorados")
    void extraFieldsAreIgnored() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(1L, "Maria Silva", "maria@email.com",
                Role.CLIENT, null, null, null, true, null, null));

        mockMvc.perform(post("/ServiceHub/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":999,"active":false,"fullName":"Maria Silva","email":"maria@email.com",
                         "password":"Senha123","role":"CLIENT"}"""))
                .andExpect(status().isCreated());
    }
}
