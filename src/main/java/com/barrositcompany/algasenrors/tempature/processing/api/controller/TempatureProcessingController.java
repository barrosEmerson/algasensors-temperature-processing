package com.barrositcompany.algasenrors.tempature.processing.api.controller;

import com.barrositcompany.algasenrors.tempature.processing.api.model.TemperatureLogOutputDTO;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static com.barrositcompany.algasenrors.tempature.processing.api.infrastructure.rabbitmq.RabbitMQConfig.FANOUT_EXCHANGE_NAME;

@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
@Slf4j
@RequiredArgsConstructor
public class TempatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public void data(@PathVariable TSID sensorId, @RequestBody String input) {
        if(input == null || input.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input cannot be null or empty");
        }

        Double temperature;

        try {
             temperature = Double.parseDouble(input);
            // Process the temperature data here
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid temperature value: " + input, e);
        }

        TemperatureLogOutputDTO logOutputDTO = TemperatureLogOutputDTO.builder()
                .sensorId(sensorId)
                .value(temperature)
                .registeredAt(OffsetDateTime.now())
                .build();

        log.info("Received temperature data: {}", logOutputDTO);

        String exchange = FANOUT_EXCHANGE_NAME;
        String routingKey = "";
        Object message = logOutputDTO;

        MessagePostProcessor messagePostProcessor = messageProcessor -> {
            messageProcessor.getMessageProperties().setHeader("sensorId", logOutputDTO.getSensorId().toString());
            return messageProcessor;
        };
        rabbitTemplate.convertAndSend(exchange, routingKey, message, messagePostProcessor);
    }
}
