package edu.eci.arsw.calls.pubsub;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RedisPubSubBridgeTest {

    @Test
    void publishYSubscribe_deberianFuncionarEnModoLocal_sinRedis_casoFeliz1() {
        RedisPubSubBridge bridge = new RedisPubSubBridge(null, null);

        List<String> recibidos = new ArrayList<>();
        Consumer<String> consumer = recibidos::add;

        bridge.subscribe("ch1", consumer);
        bridge.publish("ch1", "hola");

        assertEquals(List.of("hola"), recibidos);
    }

    @Test
    void publish_noDeberiaFallar_sinSuscriptores_casoFeliz2() {
        RedisPubSubBridge bridge = new RedisPubSubBridge(null, null);

        assertDoesNotThrow(() -> bridge.publish("ch2", "mensaje"));
    }

    @Test
    void publish_deberiaUsarRedisCuandoEstaDisponible_casoFeliz3() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);

        RedisPubSubBridge bridge = new RedisPubSubBridge(template, container);

        bridge.subscribe("ch-redis", payload -> {});

        verify(container).addMessageListener(
                any(MessageListener.class),
                any(Topic.class)
        );

        bridge.publish("ch-redis", "hola");

        verify(template).convertAndSend("ch-redis", "hola");
    }

    @Test
    void publish_deberiaHacerFallbackLocal_siRedisFalla_casoNoFeliz() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);

        RedisPubSubBridge bridge = new RedisPubSubBridge(template, container);

        List<String> recibidos = new ArrayList<>();
        bridge.subscribe("ch-fail", recibidos::add);

        doThrow(new RuntimeException("redis down"))
                .when(template).convertAndSend("ch-fail", "msg");

        bridge.publish("ch-fail", "msg");

        assertEquals(List.of("msg"), recibidos);
    }
    @Test
    void constructor_deberiaRegistrarErrorHandler_yManejarErrores() {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        new RedisPubSubBridge(null, container);

        org.mockito.ArgumentCaptor<org.springframework.util.ErrorHandler> captor = 
            org.mockito.ArgumentCaptor.forClass(org.springframework.util.ErrorHandler.class);
        
        verify(container).setErrorHandler(captor.capture());

        captor.getValue().handleError(new RuntimeException("Redis connection lost"));
    }

    @Test
    void subscribe_deberiaManejarExcepcionAlSuscribirse_yHacerFallback() {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RedisPubSubBridge bridge = new RedisPubSubBridge(template, container);

        doThrow(new RuntimeException("Error al suscribir"))
            .when(container).addMessageListener(any(MessageListener.class), any(Topic.class));

        assertDoesNotThrow(() -> bridge.subscribe("ch-error", msg -> {}));
        
        verify(container).addMessageListener(any(MessageListener.class), any(Topic.class));
    }

    @Test
    void alRecibirMensajeDeRedis_deberiaInvocarConsumidoresLocales() {
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        RedisPubSubBridge bridge = new RedisPubSubBridge(null, container);
        List<String> recibidos = new ArrayList<>();

        bridge.subscribe("ch-inbound", recibidos::add);

        org.mockito.ArgumentCaptor<MessageListener> captor = 
            org.mockito.ArgumentCaptor.forClass(MessageListener.class);
        
        verify(container).addMessageListener(captor.capture(), any(Topic.class));
        MessageListener listenerRegistrado = captor.getValue();

        org.springframework.data.redis.connection.Message mockMessage = 
            mock(org.springframework.data.redis.connection.Message.class);
        
        when(mockMessage.getBody()).thenReturn("mensaje-desde-redis".getBytes());

        listenerRegistrado.onMessage(mockMessage, null);

        assertEquals(1, recibidos.size());
        assertEquals("mensaje-desde-redis", recibidos.get(0));
    }

    @Test
    void fanoutLocal_deberiaManejarExcepcionDeConsumidor_sinRomperOtros() {
        RedisPubSubBridge bridge = new RedisPubSubBridge(null, null);
        List<String> recibidos = new ArrayList<>();

        Consumer<String> consumidorMalo = msg -> { throw new RuntimeException("Fallo intencional"); };
        Consumer<String> consumidorBueno = recibidos::add;

        bridge.subscribe("ch-mixed", consumidorMalo);
        bridge.subscribe("ch-mixed", consumidorBueno);

        assertDoesNotThrow(() -> bridge.publish("ch-mixed", "test"));

        assertEquals(1, recibidos.size());
        assertEquals("test", recibidos.get(0));
    }
}
