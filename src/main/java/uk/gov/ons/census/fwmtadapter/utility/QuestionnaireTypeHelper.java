package uk.gov.ons.census.fwmtadapter.utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuestionnaireTypeHelper {

  private static final String ENGLAND_HOUSEHOLD_CONTINUATION = "11";
  private static final String WALES_HOUSEHOLD_CONTINUATION = "12";
  private static final String WALES_HOUSEHOLD_CONTINUATION_WELSH = "13";
  private static final String NORTHERN_IRELAND_HOUSEHOLD_CONTINUATION = "14";
  private static final String CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES = "61";
  private static final String CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_WALES_WELSH = "63";
  private static final Set<String> continuationQuestionnaireTypes =
      new HashSet<>(
          Arrays.asList(
              ENGLAND_HOUSEHOLD_CONTINUATION,
              WALES_HOUSEHOLD_CONTINUATION,
              WALES_HOUSEHOLD_CONTINUATION_WELSH,
              NORTHERN_IRELAND_HOUSEHOLD_CONTINUATION,
              CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES,
              CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_WALES_WELSH));

  public static boolean isContinuationQuestionnaireType(String questionnaireId) {
    String questionnaireType = questionnaireId.substring(0, 2);

    return continuationQuestionnaireTypes.contains(questionnaireType);
  }
}
