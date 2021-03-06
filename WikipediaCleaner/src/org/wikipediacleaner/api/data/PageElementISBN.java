/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2013  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */


package org.wikipediacleaner.api.data;

import java.util.ArrayList;
import java.util.List;

import org.wikipediacleaner.api.constants.WPCConfiguration;
import org.wikipediacleaner.api.constants.WPCConfigurationStringList;


/**
 * Class containing information about an ISBN.
 */
public class PageElementISBN extends PageElement {

  /**
   * ISBN prefix.
   */
  private final static String ISBN_PREFIX = "ISBN";

  /**
   * ISBN possible meaningful characters.
   */
  private final static String POSSIBLE_CHARACTERS = "0123456789Xx";

  /**
   * ISBN possible extraneous characters.
   */
  private final static String EXTRA_CHARACTERS = "-  ";

  /**
   * ISBN incorrect characters.
   */
  private final static String INCORRECT_CHARACTERS = ":‐\t—=–#";

  /**
   * ISBN incorrect characters at the beginning.
   */
  private final static String INCORRECT_BEGIN_CHARACTERS = ":;‐\t—=–#";

  /**
   * @param analysis Page analysis.
   * @return List of ISBN.
   */
  public static List<PageElementISBN> analyzePage(
      PageAnalysis analysis) {
    List<PageElementISBN> isbns = new ArrayList<PageElementISBN>();

    // Search for ISBN templates
    WPCConfiguration config = analysis.getWPCConfiguration();
    List<String[]> isbnTemplates = config.getStringArrayList(WPCConfigurationStringList.ISBN_TEMPLATES);
    if (isbnTemplates != null) {
      for (String[] isbnTemplate : isbnTemplates) {
        if (isbnTemplate.length > 0) {
          List<PageElementTemplate> templates = analysis.getTemplates(isbnTemplate[0]);
          if (templates != null) {
            for (PageElementTemplate template : templates) {
              String[] params = null;
              if (isbnTemplate.length > 1) {
                params = isbnTemplate[1].split(",");
              } else {
                params = new String[]{ "1" };
              }
              for (String param : params) {
                if ((param != null) && (param.length() > 0)) {
                  analyzeTemplateParams(
                      analysis, isbns, template, param,
                      false, false, true, false);
                }
              }
            }
          }
        }
      }
    }

    // Search for ISBN templates where help is requested
    isbnTemplates = config.getStringArrayList(WPCConfigurationStringList.ISBN_HELP_NEEDED_TEMPLATES);
    if (isbnTemplates != null) {
      for (String[] isbnTemplate : isbnTemplates) {
        if (isbnTemplate.length > 0) {
          List<PageElementTemplate> templates = analysis.getTemplates(isbnTemplate[0]);
          if (templates != null) {
            for (PageElementTemplate template : templates) {
              analyzeTemplateParams(
                  analysis, isbns, template,
                  ((isbnTemplate.length > 1) && (isbnTemplate[1].length() > 0)) ? isbnTemplate[1] : "1",
                  false, false, false, true);
            }
          }
        }
      }
    }

    // Search for ISBN in template parameters
    List<PageElementTemplate> templates = analysis.getTemplates();
    for (PageElementTemplate template : templates) {
      analyzeTemplateParams(analysis, isbns, template, "ISBN", true, true, true, false);
    }

    // Search for ISBN in plain texts
    String contents = analysis.getContents();
    if (contents == null) {
      return isbns;
    }
    int index = 0;
    int maxIndex = contents.length() - ISBN_PREFIX.length();
    while (index < maxIndex) {

      // Check if it's a potential ISBN
      boolean isValid = true;
      boolean isISBN = ISBN_PREFIX.equalsIgnoreCase(
          contents.substring(index, index + ISBN_PREFIX.length()));
      if (isISBN && (analysis.isInComment(index) != null)) {
        isISBN = false;
      }
      if (isISBN && (analysis.isInTag(index) != null)) {
        isISBN = false;
      }
      if (isISBN && (analysis.getSurroundingTag(PageElementTag.TAG_WIKI_NOWIKI, index) != null)) {
        isISBN = false;
      }
      if (isISBN && isInISBN(index, isbns)) {
        isISBN = false;
      }
      if (isISBN) {
        PageElementExternalLink link = analysis.isInExternalLink(index);
        if (link != null) {
          if (!link.hasSquare() ||
              (index < link.getBeginIndex() + link.getTextOffset()) ||
              (link.getText() == null)) {
            isValid = false;
          }
        }
      }
      if (isISBN) {
        PageElementTemplate template = analysis.isInTemplate(index);
        if (template != null) {
          if ((template.getParameterCount() == 0) ||
              (index < template.getParameterPipeIndex(0))) {
            isISBN = false;
          }
        }
      }
      if (isISBN) {
        PageElementImage image = analysis.isInImage(index);
        if (image != null) {
          if (index < image.getBeginIndex() + image.getFirstPipeOffset()) {
            isISBN = false;
          }
        }
      }

      if (isISBN) {

        // Check if it's a template parameter
        boolean parameter = false;
        PageElementTemplate template = analysis.isInTemplate(index);
        if (template != null) {
          for (int paramNum = 0; paramNum < template.getParameterCount(); paramNum++) {
            if ((template.getParameterPipeIndex(paramNum) < index) &&
                (template.getParameterValueStartIndex(paramNum) > index)) {
              parameter = true;
            }
          }
        }

        int beginIndex = index;
        index += ISBN_PREFIX.length();
        if (!parameter) {
          boolean correct = true;
          if ((beginIndex >= 2) && (index + 2 < contents.length())) {
            if (contents.startsWith("[[", beginIndex - 2) &&
                contents.startsWith("]]", index)) {
              correct = false;
              beginIndex -= 2;
              index += 2;
            }
          }
          if (beginIndex >= 3) {
            if (contents.startsWith("10-", beginIndex - 3) ||
                contents.startsWith("13-", beginIndex - 3)) {
              correct = false;
              beginIndex -= 3;
            }
          }
          boolean spaceFound = false;
          if (analysis.isInComment(index) == null) {
            while ((index < contents.length()) && (contents.charAt(index) == ' ')) {
              index++;
              spaceFound = true;
            }
            while ((index < contents.length()) &&
                (INCORRECT_BEGIN_CHARACTERS.indexOf(contents.charAt(index)) >= 0)) {
              index++;
              correct = false;
            }
          }
          int beginNumber = -1;
          int endNumber = beginNumber;
          boolean finished = false;
          correct &= spaceFound;
          boolean nextCorrect = correct;
          while (!finished && (index < contents.length())) {
            char currentChar = contents.charAt(index);
            if (POSSIBLE_CHARACTERS.indexOf(currentChar) >= 0) {
              if (beginNumber < 0) {
                beginNumber = index;
              }
              endNumber = index + 1;
              index++;
              correct = nextCorrect;
            } else if (EXTRA_CHARACTERS.indexOf(currentChar) >= 0) {
              if (beginNumber < 0) {
                nextCorrect = false;
              }
              index++;
            } else if (INCORRECT_CHARACTERS.indexOf(currentChar) >= 0) {
              index++;
              nextCorrect = false;
            } else {
              if ((endNumber == index) && (Character.isLetter(currentChar))) {
                correct = false;
              }
              finished = true;
            }
          }
          if (endNumber > beginNumber) {
            String number = contents.substring(beginNumber, endNumber);
            isbns.add(new PageElementISBN(
                beginIndex, endNumber, analysis, number,
                isValid, correct, false, null));
            index = endNumber;
          }
        }
      } else {
        index++;
      }
    }

    return isbns;
  }

  /**
   * @param index Current index.
   * @param isbns List of ISBN.
   * @return True if the current index is already in a ISBN.
   */
  private static boolean isInISBN(int index, List<PageElementISBN> isbns) {
    if (isbns != null) {
      for (PageElementISBN tmpIsbn : isbns) {
        if ((tmpIsbn.getBeginIndex() <= index) &&
            (tmpIsbn.getEndIndex() > index)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if template parameter is an ISBN.
   * 
   * @param analysis Page analysis.
   * @param isbns Current list of ISBN.
   * @param template Template.
   * @param argumentName Template parameter name.
   * @param ignoreCase True if parameter name should compared ignoring case.
   * @param acceptNumbers True if numbers are accepted after parameter name.
   * @param acceptAllValues True if all values are accepted, even if not compatible with ISBN. 
   * @param helpRequested True if help has been requested for this ISBN.
   */
  private static void analyzeTemplateParams(
      PageAnalysis analysis, List<PageElementISBN> isbns,
      PageElementTemplate template,
      String argumentName,
      boolean ignoreCase, boolean acceptNumbers,
      boolean acceptAllValues, boolean helpRequested) {
    int paramDefaultName = 1;
    for (int paramNum = 0; paramNum < template.getParameterCount(); paramNum++) {

      // Check parameter name
      String paramName = template.getParameterName(paramNum);
      if ((paramName == null) || (paramName.trim().length() == 0)) {
        paramName = Integer.toString(paramDefaultName);
        paramDefaultName++;
      }
      boolean nameOk = false;
      if ((ignoreCase && argumentName.equalsIgnoreCase(paramName)) ||
          (argumentName.equals(paramName))) {
        nameOk = true;
      } else if (acceptNumbers && (paramName.length() > argumentName.length())) {
        String shortParamName = paramName.substring(0, argumentName.length());
        if ((ignoreCase && argumentName.equalsIgnoreCase(shortParamName)) ||
            (argumentName.equals(paramName))) {
          nameOk = true;
          for (int i = argumentName.length(); i < paramName.length(); i++) {
            if (!Character.isDigit(paramName.charAt(i))) {
              nameOk = false;
            }
          }
        }
      }
      
      if (nameOk) {
        String paramValue = template.getParameterValue(paramNum);
        boolean ok = true;
        boolean hasDigit = false;
        int i = 0;
        int beginIndex = -1;
        int endIndex = -1;
        boolean correct = true;
        while (ok && (i < paramValue.length())) {
          char currentChar = paramValue.charAt(i);
          if (POSSIBLE_CHARACTERS.indexOf(currentChar) >= 0) {
            if (Character.isDigit(currentChar)) {
              if (beginIndex < 0) {
                beginIndex = i;
              }
              endIndex = i + 1;
              hasDigit = true;
            } else if (Character.toUpperCase(currentChar) == 'X') {
              endIndex = i + 1;
            }
            i++;
          } else if (EXTRA_CHARACTERS.indexOf(currentChar) >= 0) {
            i++;
          } else if (INCORRECT_CHARACTERS.indexOf(currentChar) >= 0) {
            i++;
            correct = false;
          } else {
            ok = false;
          }
        }
        int delta = template.getParameterValueStartIndex(paramNum);
        if (beginIndex < 0) {
          beginIndex = 0;
        }
        beginIndex += delta;
        if (endIndex < 0) {
          endIndex = 0;
        }
        endIndex += delta;
        if (beginIndex < 0) {
          ok = false;
        } else {
          if (!ok && hasDigit && (paramValue.charAt(i) == '<')) {
            PageElementComment comment = analysis.isInComment(beginIndex + i);
            if ((comment != null) &&
                (comment.getBeginIndex() == beginIndex + i)) {
              ok = true;
              i += comment.getEndIndex() - comment.getBeginIndex();
              while (ok && (i < paramValue.length())) {
                char currentChar = paramValue.charAt(i);
                if (currentChar == '<') {
                  comment = analysis.isInComment(beginIndex + i);
                  if (comment != null) {
                    i += comment.getEndIndex() - comment.getBeginIndex();
                  } else {
                    ok = false;
                  }
                } else if ((currentChar != ' ') && (currentChar != '\n')) {
                  ok = false;
                } else {
                  i++;
                }
              }
            }
          }
        }
        if (ok) {
          String contents = analysis.getContents();
          String value = contents.substring(beginIndex, endIndex);

          // Detect prefixes "10-" and "13-" before templates
          if (correct) {
            int tmpIndex = template.getBeginIndex();
            while ((tmpIndex > 0) && (contents.charAt(tmpIndex - 1) == ' ')) {
              tmpIndex--;
            }
            if ((tmpIndex > 0) && (contents.charAt(tmpIndex - 1) == '-')) {
              tmpIndex--;
              while ((tmpIndex > 0) && (contents.charAt(tmpIndex - 1) == ' ')) {
                tmpIndex--;
              }
              if (tmpIndex >= 2) {
                if (contents.startsWith("10", tmpIndex - 2) ||
                    contents.startsWith("13", tmpIndex - 2)) {
                  if ((tmpIndex == 2) || !Character.isDigit(contents.charAt(tmpIndex - 3))) {
                    correct = false;
                    beginIndex = tmpIndex - 2;
                    endIndex = template.getEndIndex();
                  }
                }
              }
            }
          }

          if (paramValue.length() > 0) {
            isbns.add(new PageElementISBN(
                beginIndex, endIndex, analysis, value,
                true, correct, helpRequested, template));
          }
        } else if (acceptAllValues) {
          if (paramValue.length() > 0) {
            isbns.add(new PageElementISBN(
                template.getParameterValueStartIndex(paramNum),
                template.getParameterValueStartIndex(paramNum) + paramValue.length(),
                analysis, paramValue, true, false, false, template));
          }
        }
      }
    }
  }

  /** WPCleaner configuration */
  private final WPCConfiguration wpcConfiguration;

  /** Full text */
  private final String fullText;

  /** ISBN not trimmed */
  private final String isbnNotTrimmed;

  /** ISBN (trimmed) */
  private final String isbn;

  /** True if ISBN is in a valid location */
  private final boolean isValid;

  /** True if ISBN syntax is correct */
  private final boolean isCorrect;

  /** Template if ISBN is a template parameter (ISBN=...) */
  private final PageElementTemplate template;

  /** True if help has been requested for this ISBN */
  private final boolean helpRequested;

  /**
   * @param beginIndex Begin index.
   * @param endIndex End index.
   * @param isbn ISBN.
   * @param isValid True if ISBN is in a valid location.
   * @param isCorrect True if ISBN syntax is correct.
   * @param helpRequested True if help has been requested for this ISBN. 
   * @param template Template if ISBN is a template parameter.
   */
  private PageElementISBN(
      int beginIndex, int endIndex, PageAnalysis analysis,
      String isbn, boolean isValid,
      boolean isCorrect, boolean helpRequested,
      PageElementTemplate template) {
    super(beginIndex, endIndex);
    this.wpcConfiguration = analysis.getWPCConfiguration();
    this.fullText = analysis.getContents().substring(beginIndex, endIndex);
    this.isbnNotTrimmed = isbn;
    this.isbn = cleanISBN(isbn);
    this.isValid = isValid;
    this.isCorrect = isCorrect;
    this.helpRequested = helpRequested;
    this.template = template;
  }

  /**
   * @return ISBN not trimmed.
   */
  public String getISBNNotTrimmed() {
    return isbnNotTrimmed;
  }

  /**
   * @return ISBN (trimmed).
   */
  public String getISBN() {
    return isbn;
  }

  /**
   * @return True if ISBN is in a valid location.
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * @return True if ISBN syntax is correct.
   */
  public boolean isCorrect() {
    return isCorrect;
  }

  /**
   * @return True if help has been requested for this ISBN.
   */
  public boolean helpRequested() {
    return helpRequested;
  }

  /**
   * @return True if ISBN is a template parameter.
   */
  public boolean isTemplateParameter() {
    return (template != null);
  }

  /**
   * @return List of possible ISBN.
   */
  public List<String> getCorrectISBN() {
    List<String> result = new ArrayList<String>();
    String prefix = isTemplateParameter() ? "" : "ISBN ";

    // Prefix outside the template
    if ((template != null) &&
        (getBeginIndex() < template.getBeginIndex())) {
      if (fullText != null) {
        result.add(fullText.substring(template.getBeginIndex() - getBeginIndex()));
      }
      return result;
    }

    // Construct a basic ISBN number
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < isbnNotTrimmed.length(); i++) {
      char currentChar = isbnNotTrimmed.charAt(i);
      if ((POSSIBLE_CHARACTERS.indexOf(currentChar) >= 0) ||
          (EXTRA_CHARACTERS.indexOf(currentChar) >= 0)) {
        buffer.append(currentChar);
      } else if ((currentChar == '‐') ||
                 (currentChar == '–') ||
                 (currentChar == '.')) {
        buffer.append("-");
      } else if (currentChar == '\t') {
        buffer.append(" ");
      } else {
        buffer.append(currentChar);
      }
    }
    String cleanedISBN = buffer.toString().trim();

    // ISBN-10
    if ((isbn.length() == 12) && (isbn.startsWith("10"))) {
      boolean ok = true;
      int index = 0;
      while ((index < cleanedISBN.length()) &&
             ((Character.isWhitespace(cleanedISBN.charAt(index))) ||
              (cleanedISBN.charAt(index) == '-'))) {
        index++;
      }
      if ((index < cleanedISBN.length() && (cleanedISBN.charAt(index) == '1'))) {
        index++;
      } else {
        ok = false;
      }
      while ((index < cleanedISBN.length()) &&
             ((Character.isWhitespace(cleanedISBN.charAt(index))) ||
              (cleanedISBN.charAt(index) == '-'))) {
        index++;
      }
      if ((index < cleanedISBN.length() && (cleanedISBN.charAt(index) == '0'))) {
        index++;
      } else {
        ok = false;
      }
      while ((index < cleanedISBN.length()) &&
             ((!Character.isDigit(cleanedISBN.charAt(index))) &&
              (cleanedISBN.charAt(index) != 'X'))) {
        index++;
      }
      if (ok && (index < cleanedISBN.length())) {
        addCorrectISBN(result, prefix, cleanedISBN.substring(index));
      }
    }

    // ISBN-13
    if ((isbn.length() == 15) && (isbn.startsWith("13"))) {
      boolean ok = true;
      int index = 0;
      while ((index < cleanedISBN.length()) &&
             ((Character.isWhitespace(cleanedISBN.charAt(index))) ||
              (cleanedISBN.charAt(index) == '-'))) {
        index++;
      }
      if ((index < cleanedISBN.length() && (cleanedISBN.charAt(index) == '1'))) {
        index++;
      } else {
        ok = false;
      }
      while ((index < cleanedISBN.length()) &&
             ((Character.isWhitespace(cleanedISBN.charAt(index))) ||
              (cleanedISBN.charAt(index) == '-'))) {
        index++;
      }
      if ((index < cleanedISBN.length() && (cleanedISBN.charAt(index) == '3'))) {
        index++;
      } else {
        ok = false;
      }
      while ((index < cleanedISBN.length()) &&
             ((!Character.isDigit(cleanedISBN.charAt(index))) &&
              (cleanedISBN.charAt(index) != 'X'))) {
        index++;
      }
      if (ok && (index < cleanedISBN.length())) {
        addCorrectISBN(result, prefix, cleanedISBN.substring(index));
      }
    }

    // Basic replacement
    addCorrectISBN(result, prefix, cleanedISBN);

    // Common mistyped characters
    cleanedISBN = cleanedISBN.replaceAll("O", "0");
    cleanedISBN = cleanedISBN.replaceAll("I", "1");
    cleanedISBN = cleanedISBN.replaceAll("B", "8");
    addCorrectISBN(result, prefix, cleanedISBN);

    return result;
  }

  /**
   * @param result List of possible replacements.
   * @param prefix ISBN prefix.
   * @param cleanedISBN Cleaned up ISBN.
   */
  private void addCorrectISBN(List<String> result, String prefix, String cleanedISBN) {
    addCorrectISBN(result, prefix + cleanedISBN);
    if (!isTemplateParameter()) {
      List<String[]> isbnTemplates = wpcConfiguration.getStringArrayList(
          WPCConfigurationStringList.ISBN_TEMPLATES);
      if (isbnTemplates != null) {
        for (String[] isbnTemplate : isbnTemplates) {
          if (isbnTemplate.length > 2) {
            String[] params = isbnTemplate[1].split(",");
            Boolean suggested = Boolean.valueOf(isbnTemplate[2]);
            if ((params.length > 0) && (Boolean.TRUE.equals(suggested))) {
              StringBuilder buffer = new StringBuilder();
              buffer.append("{{");
              buffer.append(isbnTemplate[0]);
              buffer.append("|");
              if (!"1".equals(params[0])) {
                buffer.append(params[0]);
                buffer.append("=");
              }
              buffer.append(cleanedISBN);
              buffer.append("}}");
              addCorrectISBN(result, buffer.toString());
            }
          }
        }
      }
      
    }
  }

  /**
   * @param result List of possible replacements.
   * @param correctISBN Possible replacement.
   */
  private void addCorrectISBN(List<String> result, String correctISBN) {
    if ((result == null) || (correctISBN == null)) {
      return;
    }
    if (!result.contains(correctISBN)) {
      result.add(correctISBN);
    }
  }

  /**
   * @param helpNeededTemplate Name of template for asking for help.
   * @param reason Reason of the request.
   * @return Text for requesting for help.
   */
  public String askForHelp(
      String[] helpNeededTemplate, String reason) {
    if ((helpNeededTemplate == null) ||
        (helpNeededTemplate.length == 0)) {
      return null;
    }
    if (isTemplateParameter()) {
      return null;
    }

    // Template name
    StringBuilder replacement = new StringBuilder();
    replacement.append("{{");
    replacement.append(helpNeededTemplate[0]);

    // ISBN
    replacement.append("|");
    if ((helpNeededTemplate.length > 1) &&
        (helpNeededTemplate[1].length() > 0)) {
      replacement.append(helpNeededTemplate[1]);
      replacement.append("=");
    }
    replacement.append(getISBNNotTrimmed());

    // Reason
    if ((reason != null) &&
        (helpNeededTemplate.length > 2) &&
        (helpNeededTemplate[2].length() > 0)) {
      replacement.append("|");
      replacement.append(helpNeededTemplate[2]);
      replacement.append("=");
      replacement.append(reason);
    }

    // Extra parameters
    for (int i = 3; i < helpNeededTemplate.length; i++) {
      replacement.append("|");
      replacement.append(helpNeededTemplate[i]);
    }

    replacement.append("}}");
    return replacement.toString();
  }

  /**
   * @param comment Comment for asking for help.
   * @param reason Reason of the request.
   * @return Text for requesting for help.
   */
  public String askForHelp(
      String comment, String reason) {
    if ((comment == null) ||
        (comment.trim().length() == 0)) {
      return null;
    }
    StringBuilder replacement = new StringBuilder();
    replacement.append("<!-- ");
    replacement.append(comment);
    if ((reason != null) && (reason.trim().length() > 0)) {
      replacement.append(" - ");
      replacement.append(reason);
    }
    replacement.append(" -->");
    return replacement.toString();
  }

  /**
   * @param isbn ISBN number.
   * @return Cleaned up ISBN number.
   */
  public static String cleanISBN(String isbn) {
    if (isbn == null) {
      return null;
    }
    isbn = isbn.trim();
    if (isbn.length() == 0) {
      return isbn;
    }
    PageAnalysis analysis = new PageAnalysis(null, isbn);
    StringBuilder result = new StringBuilder();
    int i = 0;
    while (i < isbn.length()) {
      char current = Character.toUpperCase(isbn.charAt(i));
      if (current == '<') {
        PageElementComment comment = analysis.isInComment(i);
        if ((comment != null) && (comment.getBeginIndex() == i)) {
          i = comment.getEndIndex() - 1;
        } else {
          PageElementTag refTag = analysis.isInTag(i, PageElementTag.TAG_WIKI_REF);
          if ((refTag != null) && (refTag.getBeginIndex() == i)) {
            i = refTag.getCompleteEndIndex() - 1;
          }
        }
      } else if (POSSIBLE_CHARACTERS.indexOf(current) >= 0) {
        result.append(current);
      }
      i++;
    }
    return result.toString();
  }

  /**
   * @param isbnValue ISBN value.
   * @return Computed checksum.
   */
  public static char computeChecksum(String isbnValue) {
    if (isbnValue == null) {
      return 0;
    }
    isbnValue = cleanISBN(isbnValue);

    // Check for ISBN-10
    if (isbnValue.length() == 10) {
      int check = 0;
      for (int i = 0; i < 9; i++) {
        char currentChar = isbnValue.charAt(i);
        if (Character.isDigit(currentChar)) {
          check += (10 - i) * (currentChar - '0');
        } else {
          return 0;
        }
      }
      check = check % 11; // Modulus 11
      check = 11 - check; // Invert
      check = check % 11; // 11 -> 0
      char checksum = (check < 10) ? (char) ('0' + check): 'X';
      return checksum;
    }

    // Check for ISBN-13
    if (isbnValue.length() == 13) {
      int check = 0;
      for (int i = 0; i < 12; i++) {
        char currentChar = isbnValue.charAt(i);
        if (Character.isDigit(currentChar)) {
          check += ((i % 2 == 0) ? 1 : 3) * (currentChar - '0');
        } else {
          return 0;
        }
      }
      check = check % 10; // Modulus 10
      check = 10 - check; // Invert
      check = check % 10; // 10 -> 0
      char checksum = (char) ('0' + check);
      return checksum;
    }

    return 0;
  }

  /**
   * @param isbnValue ISBN value.
   * @return True if ISBN value is valid.
   */
  public static boolean isValid(String isbnValue) {
    if (isbnValue == null) {
      return false;
    }
    isbnValue = cleanISBN(isbnValue);
    if ((isbnValue.length() != 10) && (isbnValue.length() != 13)) {
      return false;
    }
    if (isbnValue.charAt(isbnValue.length() - 1) != computeChecksum(isbnValue)) {
      return false;
    }
    return true;
  }
}
