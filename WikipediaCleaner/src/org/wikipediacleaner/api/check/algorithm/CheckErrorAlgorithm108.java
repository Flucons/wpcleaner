/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2013  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */

package org.wikipediacleaner.api.check.algorithm;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.wikipediacleaner.api.check.CheckErrorResult;
import org.wikipediacleaner.api.check.NullActionProvider;
import org.wikipediacleaner.api.data.PageAnalysis;
import org.wikipediacleaner.api.data.PageElementISSN;
import org.wikipediacleaner.api.data.PageElementTemplate;
import org.wikipediacleaner.i18n.GT;


/**
 * Algorithm for analyzing error 108 of check wikipedia project.
 * Error 108: ISSN wrong checksum
 */
public class CheckErrorAlgorithm108 extends CheckErrorAlgorithmISSN {

  public CheckErrorAlgorithm108() {
    super("ISSN wrong checksum");
  }

  /**
   * Analyze a page to check if errors are present.
   * 
   * @param analysis Page analysis.
   * @param errors Errors found in the page.
   * @param onlyAutomatic True if analysis could be restricted to errors automatically fixed.
   * @return Flag indicating if the error was found.
   */
  @Override
  public boolean analyze(
      PageAnalysis analysis,
      Collection<CheckErrorResult> errors, boolean onlyAutomatic) {
    if (analysis == null) {
      return false;
    }

    // Analyze each ISSN
    boolean result = false;
    List<PageElementISSN> issns = analysis.getISSNs();
    for (PageElementISSN issn : issns) {
      String number = issn.getISSN();
      if ((number != null) && (number.length() == 8)) {
        char check = Character.toUpperCase(number.charAt(7));
        char computedCheck = Character.toUpperCase(
            PageElementISSN.computeChecksum(number));

        String message = null;
        if ((check != computedCheck) &&
            (Character.isDigit(computedCheck) || (computedCheck == 'X'))) {
          message = GT._(
              "The checksum is {0} instead of {1}",
              new Object[] { check, computedCheck } );
        }

        if (message != null) {
          if (errors == null) {
            return true;
          }
          result = true;
          CheckErrorResult errorResult = createCheckErrorResult(analysis, issn, true);
          errorResult.addPossibleAction(message, new NullActionProvider());
          addHelpNeededTemplates(analysis, errorResult, issn);
          addHelpNeededComment(analysis, errorResult, issn);

          // Add original ISSN
          String originalValue = issn.getISSN();
          addSearchEngines(analysis, errorResult, originalValue);

          // Add search engines using other parameters of the template
          if (issn.isTemplateParameter()) {
            PageElementTemplate template = analysis.isInTemplate(issn.getBeginIndex());
            addSearchEngines(analysis, errorResult, template);
          }

          // Add ISSN with modified checksum
          List<String> searchISSN = new ArrayList<>();
          if (computedCheck != check) {
            String value = originalValue.substring(0, originalValue.length() - 1) + computedCheck;
            addSearchISSN(searchISSN, value, false);
          }

          // Add ISSN with characters inversion
          if (originalValue.length() == 8) {
            int previousChar = -1;
            for (int currentChar = 0; currentChar < originalValue.length(); currentChar++) {
              if (Character.isDigit(originalValue.charAt(currentChar))) {
                if (previousChar >= 0) {
                  String value =
                      originalValue.substring(0, previousChar) +
                      originalValue.charAt(currentChar) +
                      originalValue.substring(previousChar + 1, currentChar) +
                      originalValue.charAt(previousChar) +
                      originalValue.substring(currentChar + 1);
                  addSearchISSN(searchISSN, value, false);
                }
                previousChar = currentChar;
              }
            }
          }

          // Add ISSN with one modified digit
          if (originalValue.length() == 8) {
            for (int currentChar = 0; currentChar < originalValue.length(); currentChar++) {
              if (Character.isDigit(originalValue.charAt(currentChar))) {
                for (char newChar = '0'; newChar <= '9'; newChar++) {
                  String value =
                      originalValue.substring(0, currentChar) +
                      newChar +
                      originalValue.substring(currentChar + 1);
                  addSearchISSN(searchISSN, value, false);
                }
              }
            }
          }

          // Add direct search engines
          addSearchEngines(
              analysis, errorResult, searchISSN,
              GT._("Similar ISSN"));

          errors.add(errorResult);
        }
      }
    }

    return result;
  }

  /**
   * @param searchISSN List of ISSN.
   * @param issn ISSN to be added.
   * @param force True if ISSN should be added even if incorrect.
   */
  private void addSearchISSN(List<String> searchISSN, String issn, boolean force) {
    if (!searchISSN.contains(issn)) {
      if (force ||
          (PageElementISSN.computeChecksum(issn) == issn.charAt(issn.length() - 1))) {
        searchISSN.add(issn);
      }
    }
  }

  /**
   * @param issn ISSN number.
   * @return Reason for the error.
   */
  @Override
  public String getReason(PageElementISSN issn) {
    if (issn == null) {
      return null;
    }
    String number = issn.getISSN();
    if (number == null) {
      return null;
    }
    char check = Character.toUpperCase(number.charAt(7));
    char computedCheck = Character.toUpperCase(PageElementISSN.computeChecksum(number));
    if (check != computedCheck) {
      String reasonTemplate = getSpecificProperty("reason_checksum", true, true, false);
      if (reasonTemplate == null) {
        reasonTemplate = getSpecificProperty("reason", true, true, false);
      }
      if (reasonTemplate == null) {
        return null;
      }
      return MessageFormat.format(reasonTemplate, computedCheck, check);
    }

    return null;
  }

  /**
   * Return the parameters used to configure the algorithm.
   * 
   * @return Map of parameters (Name -> description).
   */
  @Override
  public Map<String, String> getParameters() {
    Map<String, String> parameters = super.getParameters();
    parameters.put(
        "reason_checksum", GT._("An explanation of the problem (incorrect checksum)"));
    return parameters;
  }
}
