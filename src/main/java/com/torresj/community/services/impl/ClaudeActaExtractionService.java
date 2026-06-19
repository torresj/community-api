package com.torresj.community.services.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.torresj.community.dtos.MeetingDraft;
import com.torresj.community.services.ActaExtractionService;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;

/**
 * Extracts acta data from a (scanned) PDF using Claude vision. The PDF pages are sent as a
 * document block and the model returns a {@link MeetingDraft} via class-based structured
 * outputs. Best-effort: any failure yields an empty draft rather than propagating.
 */
@Slf4j
public class ClaudeActaExtractionService implements ActaExtractionService {

    private static final String MODEL = "claude-opus-4-8";

    private static final String PROMPT = """
            Eres un asistente que extrae datos del acta de una junta de propietarios ("junta de vecinos")
            a partir del PDF adjunto (puede ser un documento escaneado). Devuelve únicamente los datos
            estructurados solicitados. Reglas:
            - Si un dato no aparece o no estás seguro, déjalo vacío (null). No inventes información.
            - dateTime: fecha y hora de la junta en formato ISO-8601 (yyyy-MM-ddTHH:mm) si es posible.
            - type: ORDINARIA o EXTRAORDINARIA según la convocatoria.
            - convocatoria: 1 o 2 (primera o segunda convocatoria).
            - items: cada punto del orden del día, con su número de orden, una descripción/título, notas
              con el detalle de la deliberación, y el tipo (INFO si es informativo, VOTING si se vota,
              ELECTION si es un nombramiento/elección de cargos).
            - voting (solo para puntos votados): rellena los recuentos (a favor / en contra / abstenciones)
              cuando el acta los detalle; si se aprobó "por unanimidad" marca unanimous=true y result=APPROVED;
              "no se aprueba" => result=REJECTED; si se pospone => result=POSTPONED; sin acuerdo => NO_AGREEMENT.
            """;

    private final AnthropicClient client;

    public ClaudeActaExtractionService(String apiKey) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public MeetingDraft extract(byte[] pdf) {
        try {
            String base64 = Base64.getEncoder().encodeToString(pdf);
            DocumentBlockParam document = DocumentBlockParam.builder().base64Source(base64).build();
            StructuredMessageCreateParams<MeetingDraft> params =
                    MessageCreateParams.builder()
                            .model(MODEL)
                            .maxTokens(8000L)
                            .outputConfig(MeetingDraft.class)
                            .addUserMessageOfBlockParams(List.of(
                                    ContentBlockParam.ofDocument(document),
                                    ContentBlockParam.ofText(TextBlockParam.builder().text(PROMPT).build())))
                            .build();
            return client.messages().create(params).content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(structured -> structured.text())
                    .findFirst()
                    .orElseGet(MeetingDraft::empty);
        } catch (Exception e) {
            log.error("Acta extraction via Claude failed; returning empty draft", e);
            return MeetingDraft.empty();
        }
    }
}
