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
import org.wikipediacleaner.api.data.ISBNRange;
import org.wikipediacleaner.api.data.PageAnalysis;
import org.wikipediacleaner.api.data.PageElementISBN;
import org.wikipediacleaner.api.data.PageElementISSN;
import org.wikipediacleaner.api.data.PageElementTemplate;
import org.wikipediacleaner.api.data.ISBNRange.ISBNInformation;
import org.wikipediacleaner.i18n.GT;


/**
 * Algorithm for analyzing error 73 of check wikipedia project.
 * Error 73: ISBN wrong checksum in ISBN-13
 */
public class CheckErrorAlgorithm073 extends CheckErrorAlgorithmISBN {

  public CheckErrorAlgorithm073() {
    super("ISBN wrong checksum in ISBN-13");
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

    // Analyze each ISBN
    boolean result = false;
    List<PageElementISBN> isbns = analysis.getISBNs();
    for (PageElementISBN isbn : isbns) {
      String number = isbn.getISBN();
      if ((number != null) && (number.length() == 13)) {
        char check = Character.toUpperCase(number.charAt(12));
        char computedCheck = Character.toUpperCase(
            PageElementISBN.computeChecksum(number));

        String message = null;
        if ((check != computedCheck) && Character.isDigit(computedCheck)) {
          message = GT._(
              "The checksum is {0} instead of {1}",
              new Object[] { check, computedCheck } );
        } else {
          ISBNInformation isbnInfo = ISBNRange.getInformation(number);
          if (isbnInfo != null) {
            if (isbnInfo.isInUnknownRange()) {
              message = GT._("There's no existing range for this ISBN");
            } else if (isbnInfo.isInReservedRange()) {
              message = GT._("This ISBN is inside a reserved range");
            }
          }
        }

        if (message != null) {
          if (errors == null) {
            return true;
          }
          result = true;
          CheckErrorResult errorResult = createCheckErrorResult(analysis, isbn, true);
          errorResult.addPossibleAction(message, new NullActionProvider());
          addHelpNeededTemplates(analysis, errorResult, isbn);
          addHelpNeededComment(analysis, errorResult, isbn);

          // Add original ISBN
          addSearchEngines(analysis, errorResult, number);

          // Add search engines using other parameters of the template
          if (isbn.isTemplateParameter()) {
            PageElementTemplate template = analysis.isInTemplate(isbn.getBeginIndex());
            addSearchEngines(analysis, errorResult, template);
          }

          // Add ISSN if number starts with 977=Prefix for ISSN
          if (number.startsWith("977")) { // Prefix for ISSN
            String value = number.substring(3, 10);
            char checkISSN = PageElementISSN.computeChecksum(value + '0');
            if (checkISSN > 0) {
              addSearchEnginesISSN(analysis, errorResult, value + checkISSN);
            }
          }

          // Add ISBN with modified checksum
          List<String> searchISBN = new ArrayList<>();
          if (computedCheck != check) {
            String value = number.substring(0, number.length() - 1) + computedCheck;
            addSearchISBN(searchISBN, value, false);
          }

          // Try specific replacements if ISBN doesn't start with 978 or 979
          if (!number.startsWith("978") && !number.startsWith("979")) {
            int count = 0;
            count += (number.charAt(0) == '9') ? 1 : 0;
            count += (number.charAt(1) == '7') ? 1 : 0;
            count += (number.charAt(2) == '8') ? 1 : 0;
            count += (number.charAt(2) == '9') ? 1 : 0;
            if (count == 2) {
              String value = ((number.charAt(2) == '9') ? "979" : "978") + number.substring(3);
              addSearchISBN(searchISBN, value, false);
            }
          }

          // Try ISBN-10
          if (number.startsWith("978")) {
            String value = number.substring(3);
            addSearchISBN(searchISBN, value, false);
          }

          // Add ISBN with characters inversion
          if (number.length() == 13) {
            int previousChar = -1;
            for (int currentChar = 0; currentChar < number.length(); currentChar++) {
              if (Character.isDigit(number.charAt(currentChar))) {
                if (previousChar >= 0) {
                  String value =
                      number.substring(0, previousChar) +
                      number.charAt(currentChar) +
                      number.substring(previousChar + 1, currentChar) +
                      number.charAt(previousChar) +
                      number.substring(currentChar + 1);
                  addSearchISBN(searchISBN, value, false);
                }
                previousChar = currentChar;
              }
            }
          }

          // Add ISBN with one modified digit
          if (number.length() == 13) {
            for (int currentChar = 0; currentChar < number.length(); currentChar++) {
              if (Character.isDigit(number.charAt(currentChar))) {
                for (char newChar = '0'; newChar <= '9'; newChar++) {
                  String value =
                      number.substring(0, currentChar) +
                      newChar +
                      number.substring(currentChar + 1);
                  addSearchISBN(searchISBN, value, false);
                }
              }
            }
          }

          // Add direct search engines
          addSearchEngines(
              analysis, errorResult, searchISBN,
              GT._("Similar ISBN"));

          errors.add(errorResult);
        }
      }
    }

    return result;
  }

  /**
   * @param searchISBN List of ISBN.
   * @param isbn ISBN to be added.
   * @param force True if ISBN should be added even if incorrect.
   */
  private void addSearchISBN(List<String> searchISBN, String isbn, boolean force) {
    if (!searchISBN.contains(isbn)) {
      if (force ||
          (PageElementISBN.computeChecksum(isbn) == isbn.charAt(isbn.length() - 1))) {
        searchISBN.add(isbn);
      }
    }
  }

  /**
   * @param isbn ISBN number.
   * @return Reason for the error.
   */
  @Override
  public String getReason(PageElementISBN isbn) {
    if (isbn == null) {
      return null;
    }
    String number = isbn.getISBN();
    if (number == null) {
      return null;
    }
    char check = Character.toUpperCase(number.charAt(12));
    char computedCheck = Character.toUpperCase(PageElementISBN.computeChecksum(number));

    // Invalid checksum
    if ((check != computedCheck) && Character.isDigit(computedCheck)) {
      String reasonTemplate = getSpecificProperty("reason_checksum", true, true, false);
      if (reasonTemplate == null) {
        reasonTemplate = getSpecificProperty("reason", true, true, false);
      }
      if (reasonTemplate == null) {
        return null;
      }
      return MessageFormat.format(reasonTemplate, computedCheck, check);
    }

    // Retrieve information about ISBN number
    ISBNInformation isbnInfo = ISBNRange.getInformation(number);
    if (isbnInfo != null) {
      if (isbnInfo.isInUnknownRange()) {
        String reasonTemplate = getSpecificProperty("reason_no_range", true, true, false);
        if (reasonTemplate == null) {
          return null;
        }
        return reasonTemplate;
      }

      if (isbnInfo.isInReservedRange()) {
        String reasonTemplate = getSpecificProperty("reason_reserved", true, true, false);
        if (reasonTemplate == null) {
          return null;
        }
        return reasonTemplate;
      }
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
    parameters.put(
        "reason_no_range", GT._("An explanation of the problem (non-existing range of ISBN numbers)"));
    parameters.put(
        "reason_reserved", GT._("An explanation of the problem (reserved range of ISBN numbers)"));
    return parameters;
  }
}
