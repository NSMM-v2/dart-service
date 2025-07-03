/**
 * @file KafkaProducerService.java
 * @description Kafka 메시지 생성 서비스입니다.
 *              DART API 데이터를 Kafka 토픽으로 전송합니다.
 */
package com.nsmm.esg.dart_service.kafka.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.partner-company-restore:partner-company-restore}")
    private String partnerCompanyRestoreTopic;
    
    /**
     * Kafka 토픽으로 메시지를 전송합니다.
     *
     * @param topic 메시지를 전송할 토픽
     * @param key 메시지 키
     * @param message 메시지 객체
     * @return CompletableFuture<SendResult<String, Object>> 전송 결과
     */
    public CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object message) {
        log.info("Kafka 메시지 전송 - 토픽: {}, 키: {}, 메시지: {}", topic, key, message);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("메시지 전송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("메시지 전송 실패 - 토픽: {}, 키: {}", topic, key, ex);
            }
        });
        
        return future;
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 파트너사 복원 이벤트를 Kafka 토픽으로 전송합니다.
     *
     * @param key 메시지 키 (파트너사 ID)
     * @param message 복원된 파트너사 정보
     * @return CompletableFuture<SendResult<String, Object>> 전송 결과
     */
    public CompletableFuture<SendResult<String, Object>> sendPartnerRestoreEvent(String key, Object message) {
        log.info("파트너사 복원 이벤트 전송 - 키: {}, 메시지: {}", key, message);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(partnerCompanyRestoreTopic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("파트너사 복원 이벤트 전송 성공 - 토픽: {}, 파티션: {}, 오프셋: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("파트너사 복원 이벤트 전송 실패 - 키: {}", key, ex);
            }
        });

        return future;
    }
    //------------------------------------------------------------------------------------------------------------------
} 