/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2013  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */

package org.wikipediacleaner.api.check.algorithm;

import java.util.Collection;
import java.util.List;

import org.wikipediacleaner.api.check.CheckErrorResult;
import org.wikipediacleaner.api.data.Namespace;
import org.wikipediacleaner.api.data.PageAnalysis;
import org.wikipediacleaner.api.data.PageElementTag;


/**
 * Algorithm for analyzing error 518 of check wikipedia project.
 * Error 518: nowiki tags in main namespace
 */
public class CheckErrorAlgorithm518 extends CheckErrorAlgorithmBase {

  public CheckErrorAlgorithm518() {
    super("<nowiki> tags");
  }

  /**
   * Analyze a page to check if errors are present.
   * 
   * @param analysis Page analysis.
   * @param errors Errors found in the page.
   * @return Flag indicating if the error was found.
   */
  public boolean analyze(
      PageAnalysis analysis,
      Collection<CheckErrorResult> errors) {
    if ((analysis == null) || (analysis.getPage() == null)) {
      return false;
    }
    Integer ns = analysis.getPage().getNamespace();
    if ((ns == null) || (ns.intValue() != Namespace.MAIN)) {
      return false;
    }

    // Check each tag
    List<PageElementTag> tags = analysis.getCompleteTags(PageElementTag.TAG_WIKI_NOWIKI);
    if ((tags == null) || (tags.isEmpty())) {
      return false;
    }
    if (errors == null) {
      return true;
    }
    for (PageElementTag tag : tags) {
      CheckErrorResult errorResult = createCheckErrorResult(
          analysis.getPage(), tag.getCompleteBeginIndex(), tag.getCompleteEndIndex());
      if (tag.isFullTag()) {
        errorResult.addReplacement(" ");
        errorResult.addReplacement("");
      } else if (tag.isComplete()) {
        String internalText = analysis.getContents().substring(
            tag.getValueBeginIndex(), tag.getValueEndIndex());
        int begin = tag.getBeginIndex();
        if ((begin > 0) && (analysis.getContents().charAt(begin - 1) == '\n')) {
          int index = 0;
          while ((index < internalText.length()) && (internalText.charAt(index) == ' ')) {
            index++;
          }
          if (index > 0) {
            internalText = internalText.substring(index);
          }
        }
        errorResult.addReplacement(internalText);
      } else {
        errorResult.addReplacement("");
      }
      errors.add(errorResult);
    }

    return true;
  }
}