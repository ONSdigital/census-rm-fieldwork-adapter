package uk.gov.ons.census.fwmtadapter.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import uk.gov.ons.census.fwmtadapter.utility.QuestionnaireTypeHelper;

public class QuestionnaireTypeHelperTest {

  @Test
  public void testValidQuestionnaireTypeEnglandHouseholdContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("11");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesEnglishHouseholdContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("12");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesWelshHouseholdContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("13");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeNorthernIrelandHouseholdContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("14");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeCCSPostbackEnglandAndWalesContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("61");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeCCSPostbackWalesWelshContinuation() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("63");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testIsNotContinuationQuestionnaireType() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isContinuationQuestionnaireType("99");

    // Then
    assertThat(actual).isFalse();
  }
}
