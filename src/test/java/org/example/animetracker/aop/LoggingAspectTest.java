package org.example.animetracker.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private Signature signature;

  @InjectMocks
  private LoggingAspect loggingAspect;

  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setup() {
    // Подключаем ListAppender к логгеру LoggingAspect, чтобы перехватывать сообщения
    Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @Test
  @DisplayName("logExecutionTime — должен логировать время выполнения и возвращать результат метода")
  void logExecutionTime_success() throws Throwable {
    // GIVEN
    String expectedResult = "Method Result";
    String methodSignature = "AnimeService.findAll()";

    when(signature.toShortString()).thenReturn(methodSignature);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(joinPoint.proceed()).thenReturn(expectedResult);

    // WHEN
    Object actualResult = loggingAspect.logExecutionTime(joinPoint);

    // THEN
    // Проверяем, что метод был вызван
    verify(joinPoint, times(1)).proceed();

    // Проверяем результат
    assertThat(actualResult).isEqualTo(expectedResult);

    // Проверяем логи
    assertThat(listAppender.list).hasSize(1);
    ILoggingEvent logEvent = listAppender.list.get(0);

    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
    assertThat(logEvent.getFormattedMessage())
        .contains(methodSignature)
        .contains("executed in")
        .contains("ms");
  }

  @Test
  @DisplayName("logExecutionTime — должен пробрасывать исключение, если целевой метод упал")
  void logExecutionTime_exception() throws Throwable {
    // GIVEN
    when(joinPoint.proceed()).thenThrow(new RuntimeException("Service failure"));

    // WHEN & THEN
    assertThatThrownBy(() -> loggingAspect.logExecutionTime(joinPoint))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Service failure");

    // Убеждаемся, что логирование не произошло (или произошло, если бы мы писали log в finally)
    // В текущей реализации логирование идет ПОСЛЕ proceed(), поэтому при ошибке лога не будет
    assertThat(listAppender.list).isEmpty();
  }
}