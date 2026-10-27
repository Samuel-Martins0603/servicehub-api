package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.InfoResponse;
import br.com.servicehub.api.web.PublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ServiceHub/v1/info")
@Tag(name = "Operação")
public class InfoController {

    private final String version;

    public InfoController(@Value("${app.version:1.0.0}") String version) {
        this.version = version;
    }

    @PublicEndpoint
    @GetMapping
    @Operation(summary = "Status e versão da API (público)")
    public InfoResponse info() {
        return new InfoResponse("UP", version, "ServiceHub API no ar");
    }
}
