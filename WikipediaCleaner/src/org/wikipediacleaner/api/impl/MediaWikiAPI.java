/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2013  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */

package org.wikipediacleaner.api.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.wikipediacleaner.api.API;
import org.wikipediacleaner.api.APIException;
import org.wikipediacleaner.api.CaptchaException;
import org.wikipediacleaner.api.HttpUtils;
import org.wikipediacleaner.api.RecentChangesListener;
import org.wikipediacleaner.api.constants.EnumQueryPage;
import org.wikipediacleaner.api.constants.EnumQueryResult;
import org.wikipediacleaner.api.constants.EnumWikipedia;
import org.wikipediacleaner.api.constants.WPCConfiguration;
import org.wikipediacleaner.api.constants.WPCConfigurationBoolean;
import org.wikipediacleaner.api.data.AbuseFilter;
import org.wikipediacleaner.api.data.DataManager;
import org.wikipediacleaner.api.data.LoginResult;
import org.wikipediacleaner.api.data.Page;
import org.wikipediacleaner.api.data.QueryResult;
import org.wikipediacleaner.api.data.RecentChange;
import org.wikipediacleaner.api.data.Section;
import org.wikipediacleaner.api.data.TemplateData;
import org.wikipediacleaner.api.data.User;
import org.wikipediacleaner.api.request.ApiAbuseFiltersRequest;
import org.wikipediacleaner.api.request.ApiAbuseFiltersResult;
import org.wikipediacleaner.api.request.ApiAbuseLogRequest;
import org.wikipediacleaner.api.request.ApiAbuseLogResult;
import org.wikipediacleaner.api.request.ApiAllMessagesRequest;
import org.wikipediacleaner.api.request.ApiAllMessagesResult;
import org.wikipediacleaner.api.request.ApiCategoriesRequest;
import org.wikipediacleaner.api.request.ApiCategoriesResult;
import org.wikipediacleaner.api.request.ApiCategoryMembersRequest;
import org.wikipediacleaner.api.request.ApiCategoryMembersResult;
import org.wikipediacleaner.api.request.ApiDeleteRequest;
import org.wikipediacleaner.api.request.ApiDeleteResult;
import org.wikipediacleaner.api.request.ApiEmbeddedInRequest;
import org.wikipediacleaner.api.request.ApiEmbeddedInResult;
import org.wikipediacleaner.api.request.ApiExpandRequest;
import org.wikipediacleaner.api.request.ApiExpandResult;
import org.wikipediacleaner.api.request.ApiInfoRequest;
import org.wikipediacleaner.api.request.ApiInfoResult;
import org.wikipediacleaner.api.request.ApiLanguageLinksRequest;
import org.wikipediacleaner.api.request.ApiLanguageLinksResult;
import org.wikipediacleaner.api.request.ApiLinksRequest;
import org.wikipediacleaner.api.request.ApiLinksResult;
import org.wikipediacleaner.api.request.ApiLoginRequest;
import org.wikipediacleaner.api.request.ApiLoginResult;
import org.wikipediacleaner.api.request.ApiLogoutRequest;
import org.wikipediacleaner.api.request.ApiLogoutResult;
import org.wikipediacleaner.api.request.ApiPagePropsRequest;
import org.wikipediacleaner.api.request.ApiPagePropsResult;
import org.wikipediacleaner.api.request.ApiPagesWithPropRequest;
import org.wikipediacleaner.api.request.ApiPagesWithPropResult;
import org.wikipediacleaner.api.request.ApiParseRequest;
import org.wikipediacleaner.api.request.ApiParseResult;
import org.wikipediacleaner.api.request.ApiProtectedTitlesRequest;
import org.wikipediacleaner.api.request.ApiProtectedTitlesResult;
import org.wikipediacleaner.api.request.ApiPurgeRequest;
import org.wikipediacleaner.api.request.ApiPurgeResult;
import org.wikipediacleaner.api.request.ApiBacklinksRequest;
import org.wikipediacleaner.api.request.ApiBacklinksResult;
import org.wikipediacleaner.api.request.ApiQueryPageRequest;
import org.wikipediacleaner.api.request.ApiQueryPageResult;
import org.wikipediacleaner.api.request.ApiRandomPagesRequest;
import org.wikipediacleaner.api.request.ApiRandomPagesResult;
import org.wikipediacleaner.api.request.ApiRawWatchlistRequest;
import org.wikipediacleaner.api.request.ApiRawWatchlistResult;
import org.wikipediacleaner.api.request.ApiRecentChangesRequest;
import org.wikipediacleaner.api.request.ApiRecentChangesResult;
import org.wikipediacleaner.api.request.ApiRevisionsRequest;
import org.wikipediacleaner.api.request.ApiRevisionsResult;
import org.wikipediacleaner.api.request.ApiSearchRequest;
import org.wikipediacleaner.api.request.ApiSearchResult;
import org.wikipediacleaner.api.request.ApiSiteInfoRequest;
import org.wikipediacleaner.api.request.ApiSiteInfoResult;
import org.wikipediacleaner.api.request.ApiRequest;
import org.wikipediacleaner.api.request.ApiTemplateDataRequest;
import org.wikipediacleaner.api.request.ApiTemplateDataResult;
import org.wikipediacleaner.api.request.ApiTemplatesRequest;
import org.wikipediacleaner.api.request.ApiTemplatesResult;
import org.wikipediacleaner.api.request.ApiTokensRequest;
import org.wikipediacleaner.api.request.ApiTokensResult;
import org.wikipediacleaner.api.request.ApiUsersRequest;
import org.wikipediacleaner.api.request.ApiUsersResult;
import org.wikipediacleaner.api.request.json.ApiJsonTemplateDataResult;
import org.wikipediacleaner.api.request.xml.ApiXmlAbuseFiltersResult;
import org.wikipediacleaner.api.request.xml.ApiXmlAbuseLogResult;
import org.wikipediacleaner.api.request.xml.ApiXmlAllMessagesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlCategoriesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlCategoryMembersResult;
import org.wikipediacleaner.api.request.xml.ApiXmlDeleteResult;
import org.wikipediacleaner.api.request.xml.ApiXmlEmbeddedInResult;
import org.wikipediacleaner.api.request.xml.ApiXmlExpandResult;
import org.wikipediacleaner.api.request.xml.ApiXmlInfoResult;
import org.wikipediacleaner.api.request.xml.ApiXmlLanguageLinksResult;
import org.wikipediacleaner.api.request.xml.ApiXmlLinksResult;
import org.wikipediacleaner.api.request.xml.ApiXmlLoginResult;
import org.wikipediacleaner.api.request.xml.ApiXmlLogoutResult;
import org.wikipediacleaner.api.request.xml.ApiXmlPagePropsResult;
import org.wikipediacleaner.api.request.xml.ApiXmlPagesWithPropResult;
import org.wikipediacleaner.api.request.xml.ApiXmlParseResult;
import org.wikipediacleaner.api.request.xml.ApiXmlPropertiesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlProtectedTitlesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlPurgeResult;
import org.wikipediacleaner.api.request.xml.ApiXmlBacklinksResult;
import org.wikipediacleaner.api.request.xml.ApiXmlQueryPageResult;
import org.wikipediacleaner.api.request.xml.ApiXmlRandomPagesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlRawWatchlistResult;
import org.wikipediacleaner.api.request.xml.ApiXmlRecentChangesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlResult;
import org.wikipediacleaner.api.request.xml.ApiXmlRevisionsResult;
import org.wikipediacleaner.api.request.xml.ApiXmlSearchResult;
import org.wikipediacleaner.api.request.xml.ApiXmlSiteInfoResult;
import org.wikipediacleaner.api.request.xml.ApiXmlTemplatesResult;
import org.wikipediacleaner.api.request.xml.ApiXmlTokensResult;
import org.wikipediacleaner.api.request.xml.ApiXmlUsersResult;
import org.wikipediacleaner.gui.swing.basic.Utilities;
import org.wikipediacleaner.i18n.GT;
import org.wikipediacleaner.utils.Configuration;
import org.wikipediacleaner.utils.ConfigurationValueBoolean;
import org.wikipediacleaner.utils.ConfigurationValueInteger;


/**
 * MediaWiki API implementation.
 */
public class MediaWikiAPI implements API {

  private final Log log = LogFactory.getLog(MediaWikiAPI.class);

  private final static int MAX_PAGES_PER_QUERY = 50;

  private static boolean DEBUG_XML = false;
  private static XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

  private HttpClient httpClient;

  /**
   * Time of last edit.
   */
  private LinkedList<Long> lastEditTimes = new LinkedList<Long>();
  private final Object editLock = new Object();

  /**
   * Constructor.
   * 
   * @param httpClient HTTP client.
   */
  public MediaWikiAPI(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Update configuration.
   */
  public static void updateConfiguration() {
    Configuration config = Configuration.getConfiguration();
    DEBUG_XML = config.getBoolean(
        null, ConfigurationValueBoolean.DEBUG_API);
    HttpUtils.updateConfiguration();
    ApiXmlResult.updateConfiguration();
  }

  /**
   * @return Maximum number of pages per query.
   */
  @Override
  public int getMaxPagesPerQuery() {
    return MAX_PAGES_PER_QUERY;
  }

  // ==========================================================================
  // User and login
  // ==========================================================================

  /**
   * Load Wiki configuration.
   * 
   * @param wiki Wiki.
   * @param userName User name.
   */
  @Override
  public void loadConfiguration(
      EnumWikipedia wiki,
      String userName) throws APIException {

    // Retrieve site data
    loadSiteInfo(wiki);

    // Retrieve configuration
    if (wiki.getConfigurationPage() != null) {

      // Decide which pages to be retrieved
      String configPageName = wiki.getConfigurationPage();
      Page page = DataManager.getPage(
          wiki, configPageName, null, null, null);
      Page userConfigPage = null;
      if ((userName != null) && (userName.trim().length() > 0) &&
          (wiki.getUserConfigurationPage(userName) != null) &&
          (!Page.areSameTitle(wiki.getUserConfigurationPage(userName), configPageName))) {
        userConfigPage = DataManager.getPage(
            wiki,
            wiki.getUserConfigurationPage(userName),
            null, null, null);
      }

      // Retrieve contents
      List<Page> pages = new ArrayList<Page>();
      pages.add(page);
      if (userConfigPage != null) {
        pages.add(userConfigPage);
      }
      retrieveContents(wiki, pages, false, false);

      // Set configuration
      wiki.getConfiguration().setGeneralConfiguration(
          new StringReader(page.getContents()));
      if (userConfigPage != null) {
        if (Boolean.TRUE.equals(userConfigPage.isExisting())) {
          wiki.getConfiguration().setUserConfiguration(
              new StringReader(userConfigPage.getContents()));
        }
      }
    }
  }

  /**
   * Retrieves the contents of a section in a <code>page</code>.
   * 
   * @param wikipedia Wikipedia.
   * @param page Page.
   * @param section Section number.
   * @throws APIException
   */
  @Override
  public void retrieveSectionContents(EnumWikipedia wikipedia, Page page, int section)
    throws APIException {
    Map<String, String> properties = getProperties(ApiRequest.ACTION_QUERY, true);
    properties.put("prop", "revisions|info");
    properties.put("continue", "");
    properties.put("titles", page.getTitle());
    properties.put("rvprop", "content|ids|timestamp");
    properties.put("rvsection", Integer.toString(section));
    try {
      constructContents(
          page,
          getRoot(wikipedia, properties, ApiRequest.MAX_ATTEMPTS),
          "/api/query/pages/page");
    } catch (JDOMParseException e) {
      log.error("Error retrieving page content", e);
      throw new APIException("Error parsing XML", e);
    } catch (APIException e) {
      switch (e.getQueryResult()) {
      case RV_NO_SUCH_SECTION:
        // API Bug https://bugzilla.wikimedia.org/show_bug.cgi?id=26627
        page.setExisting(Boolean.FALSE);
        page.setContents(null);
        return;

      default:
        throw e;
      }
    }
  }

  /**
   * @param wikipedia Wikipedia.
   * @param pages List of pages.
   * @throws APIException
   */
  public void retrieveContentsWithoutRedirects(EnumWikipedia wikipedia, List<Page> pages)
      throws APIException {
    Map<String, String> properties = getProperties(ApiRequest.ACTION_QUERY, true);
    properties.put("prop", "revisions");
    properties.put("continue", "");
    properties.put("rvprop", "content");
    StringBuilder titles = new StringBuilder();
    for (int i = 0; i < pages.size();) {
      titles.setLength(0);
      for (int j = 0; (j < MAX_PAGES_PER_QUERY) && (i < pages.size()); i++, j++) {
        Page p = pages.get(i);
        if (j > 0) {
          titles.append("|");
        }
        titles.append(p.getTitle());
      }
      properties.put("titles", titles.toString());
      try {
        constructContents(
            pages,
            getRoot(wikipedia, properties, ApiRequest.MAX_ATTEMPTS),
            "/api/query/pages/page");
      } catch (JDOMParseException e) {
        log.error("Error retrieving redirects", e);
        throw new APIException("Error parsing XML", e);
      }
    }
  }

  /**
   * Update a page on Wikipedia.
   * 
   * @param wikipedia Wikipedia.
   * @param page Page.
   * @param newContents New contents to use.
   * @param comment Comment.
   * @param automatic True if the modification is automatic.
   * @param forceWatch Force watching the page.
   * @return Result of the command.
   * @throws APIException
   */
  @Override
  public QueryResult updatePage(
      EnumWikipedia wikipedia, Page page,
      String newContents, String comment,
      boolean automatic, boolean forceWatch) throws APIException {
    if (page == null) {
      throw new APIException("Page is null");
    }
    if (newContents == null) {
      throw new APIException("Contents is null");
    }
    if (comment == null) {
      throw new APIException("Comment is null");
    }
    if (wikipedia.getConnection().getLgToken() == null) {
      throw new APIException("You must be logged in to update pages");
    }
    int attemptNumber = 0;
    QueryResult result = null;
    do {
      attemptNumber++;
      Map<String, String> properties = getProperties(ApiRequest.ACTION_EDIT, true);
      properties.put("assert", "user");
      if (page.getContentsTimestamp() != null) {
        properties.put("basetimestamp", page.getContentsTimestamp());
      }
      properties.put("bot", "");
      properties.put("minor", "");
      if (page.getStartTimestamp() != null) {
        properties.put("starttimestamp", page.getStartTimestamp());
      }
      properties.put("summary", comment);
      properties.put("text", newContents);
      properties.put("title", page.getTitle());
      if (wikipedia.getConnection().getEditToken() != null) {
        properties.put("token", wikipedia.getConnection().getEditToken());
      }
      properties.put("watchlist", forceWatch ? "watch" : "nochange");
      CommentDecorator commentDecorator = wikipedia.getCommentDecorator();
      if (commentDecorator != null) {
        commentDecorator.manageComment(properties, "summary", "tags", automatic);
      }
      checkTimeForEdit(wikipedia.getConnection().getUser(), page.getNamespace());
      try {
        boolean hasCaptcha = false;
        do {
          hasCaptcha = false;
          try {
            result = constructEdit(
                getRoot(wikipedia, properties, 1),
                "/api/edit");
          } catch (CaptchaException e) {
            String captchaAnswer = getCaptchaAnswer(wikipedia, e);
            if (captchaAnswer != null) {
              properties.put("captchaid", e.getId());
              properties.put("captchaword", captchaAnswer);
              hasCaptcha = true;
            } else {
              throw new APIException("CAPTCHA", e);
            }
          }
        } while (hasCaptcha);
      } catch (APIException e) {
        if (e.getHttpStatus() == HttpStatus.SC_GATEWAY_TIMEOUT) {
          log.warn("Gateway timeout, waiting to see if modification has been taken into account");
          waitBeforeRetrying();
          Page tmpPage = page.replicatePage();
          retrieveContents(wikipedia, Collections.singletonList(tmpPage), false, false);
          String tmpContents = tmpPage.getContents();
          if ((tmpContents != null) &&
              (tmpContents.equals(newContents))) {
            return QueryResult.createCorrectQuery(
                tmpPage.getPageId(), tmpPage.getTitle(),
                page.getPageId(), tmpPage.getPageId());
          }
        }
        if (attemptNumber > 1) {
          throw e;
        }
        if (e.getQueryResult() == EnumQueryResult.BAD_TOKEN) {
          waitBeforeRetrying();
          log.warn("Retrieving tokens after a BAD_TOKEN answer");
          retrieveTokens(wikipedia);
        }
      } catch (JDOMParseException e) {
        log.error("Error updating page: " + e.getMessage());
        throw new APIException("Error parsing XML", e);
      }
    } while (result == null);
    return result;
  }

  /**
   * Add a new section in a page.
   * 
   * @param wikipedia Wikipedia.
   * @param page Page.
   * @param title Title of the new section.
   * @param contents Contents.
   * @param automatic True if the modification is automatic.
   * @param forceWatch Force watching the page.
   * @return Result of the command.
   * @throws APIException
   */
  @Override
  public QueryResult addNewSection(
      EnumWikipedia wikipedia,
      Page page, String title, String contents,
      boolean automatic, boolean forceWatch) throws APIException {
    return updateSection(wikipedia, page, title, "new", contents, automatic, forceWatch);
  }

  /**
   * Update a section in a page.
   * 
   * @param wikipedia Wikipedia.
   * @param page Page.
   * @param title Title of the new section.
   * @param section Section. 
   * @param contents Contents.
   * @param automatic True if the modification is automatic.
   * @param forceWatch Force watching the page.
   * @return Result of the command.
   * @throws APIException
   */
  @Override
  public QueryResult updateSection(
      EnumWikipedia wikipedia,
      Page page, String title, int section,
      String contents,
      boolean automatic, boolean forceWatch) throws APIException {
    return updateSection(wikipedia, page, title, Integer.toString(section), contents, automatic, forceWatch);
  }

  /**
   * Update a section or create a new section in a page.
   * 
   * @param wikipedia Wikipedia.
   * @param page Page.
   * @param title Title of the new section.
   * @param section Section ("new" for a new section). 
   * @param contents Contents.
   * @param forceWatch Force watching the page.
   * @param automatic True if the modification is automatic.
   * @return Result of the command.
   * @throws APIException
   */
  private QueryResult updateSection(
      EnumWikipedia wikipedia,
      Page page, String title, String section,
      String contents,
      boolean automatic, boolean forceWatch) throws APIException {
    if (page == null) {
      throw new APIException("Page is null");
    }
    if (title == null) {
      throw new APIException("Title is null");
    }
    if (contents == null) {
      throw new APIException("Contents is null");
    }
    if (wikipedia.getConnection().getLgToken() == null) {
      throw new APIException("You must be logged in to update pages");
    }
    int attemptNumber = 0;
    QueryResult result = null;
    do {
      attemptNumber++;
      Map<String, String> properties = getProperties(ApiRequest.ACTION_EDIT, true);
      properties.put("assert", "user");
      if (page.getContentsTimestamp() != null) {
        properties.put("basetimestamp", page.getContentsTimestamp());
      }
      properties.put("bot", "");
      properties.put("minor", "");
      properties.put("section", section);
      properties.put("sectiontitle", title);
      String startTimestamp = page.getStartTimestamp();
      if ((startTimestamp != null) && !startTimestamp.isEmpty()) {
        properties.put("starttimestamp", startTimestamp);
      }
      String comment = title;
      properties.put("summary", comment);
      properties.put("text", contents);
      properties.put("title", page.getTitle());
      properties.put("token", wikipedia.getConnection().getEditToken());
      properties.put("watchlist", forceWatch ? "watch" : "nochange");
      CommentDecorator commentDecorator = wikipedia.getCommentDecorator();
      if (commentDecorator != null) {
        commentDecorator.manageComment(properties, "summary", "tags", automatic);
      }
      checkTimeForEdit(wikipedia.getConnection().getUser(), page.getNamespace());
      try {
        boolean hasCaptcha = false;
        do {
          hasCaptcha = false;
          try {
            result = constructEdit(
                getRoot(wikipedia, properties, 1),
                "/api/edit");
          } catch (CaptchaException e) {
            String captchaAnswer = getCaptchaAnswer(wikipedia, e);
            if (captchaAnswer != null) {
              properties.put("captchaid", e.getId());
              properties.put("captchaword", captchaAnswer);
              hasCaptcha = true;
            } else {
              throw new APIException("CAPTCHA", e);
            }
          }
        } while (hasCaptcha);
      } catch (APIException e) {
        if (attemptNumber > 1) {
          throw e;
        }
        if (e.getQueryResult() == EnumQueryResult.BAD_TOKEN) {
          waitBeforeRetrying();
          log.warn("Retrieving tokens after a BAD_TOKEN answer");
          retrieveTokens(wikipedia);
        }
      } catch (JDOMParseException e) {
        log.error("Error updating page: " + e.getMessage());
        throw new APIException("Error parsing XML", e);
      }
    } while (result == null);
    return result;
  }

  /**
   * Initialize the information concerning redirects.
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @throws APIException
   */
  @Override
  public void initializeRedirect(
      EnumWikipedia wiki, List<Page> pages) throws APIException {
    if ((pages == null) || (pages.isEmpty())) {
      return;
    }
    Map<String, String> properties = getProperties(ApiRequest.ACTION_QUERY, true);
    properties.put("redirects", "");
    StringBuilder titles = new StringBuilder();
    for (int i = 0; i < pages.size();) {
      titles.setLength(0);
      for (int j = 0; (j < MAX_PAGES_PER_QUERY) && (i < pages.size()); i++, j++) {
        Page p = pages.get(i);
        if (j > 0) {
          titles.append("|");
        }
        titles.append(p.getTitle());
      }
      properties.put("titles", titles.toString());
      try {
        updateRedirectStatus(
            wiki, pages,
            getRoot(wiki, properties, ApiRequest.MAX_ATTEMPTS));
      } catch (JDOMParseException e) {
        log.error("Error retrieving redirects", e);
        throw new APIException("Error parsing XML", e);
      }
    }
  }

  /**
   * @param root Root element in MediaWiki answer.
   * 
   * @param query Path to the answer.
   * @return Result of the query.
   * @throws APIException
   * @throws CaptchaException Captcha.
   */
  private QueryResult constructEdit(Element root, String query)
      throws APIException, CaptchaException {
    try {
      XPath xpa = XPath.newInstance(query);
      Element node = (Element) xpa.selectSingleNode(root);
      if (node != null) {
        XPath xpaResult = XPath.newInstance("./@result");
        String result = xpaResult.valueOf(node);
        if ("Success".equalsIgnoreCase(result)) {
          XPath xpaPageId = XPath.newInstance("./@pageid");
          Integer pageId = null;
          try {
            pageId = Integer.valueOf(xpaPageId.valueOf(node));
          } catch (NumberFormatException e) {
            //
          }
          XPath xpaPageTitle = XPath.newInstance("./@title");
          XPath xpaPageOldRevId = XPath.newInstance("./@oldrevid");
          Integer pageOldRevId = null;
          try {
            pageOldRevId = Integer.valueOf(xpaPageOldRevId.valueOf(node));
          } catch (NumberFormatException e) {
            //
          }
          XPath xpaPageNewRevId = XPath.newInstance("./@newrevid");
          Integer pageNewRevId = null;
          try {
            pageNewRevId = Integer.valueOf(xpaPageNewRevId.valueOf(node));
          } catch (NumberFormatException e) {
            //
          }
          return QueryResult.createCorrectQuery(
              pageId, xpaPageTitle.valueOf(node),
              pageOldRevId, pageNewRevId);
        } else if ("Failure".equalsIgnoreCase(result)) {
          XPath xpaCaptcha = XPath.newInstance("./captcha");
          Element captcha = (Element) xpaCaptcha.selectSingleNode(node);
          if (captcha != null) {
            XPath xpaType = XPath.newInstance("./@type");
            CaptchaException exception = new CaptchaException("Captcha", xpaType.valueOf(captcha));
            XPath xpaMime = XPath.newInstance("./@mime");
            exception.setMime(xpaMime.valueOf(captcha));
            XPath xpaId = XPath.newInstance("./@id");
            exception.setId(xpaId.valueOf(captcha));
            XPath xpaUrl = XPath.newInstance("./@url");
            exception.setURL(xpaUrl.valueOf(captcha));
            throw exception;
          }
          XPath xpaSpamBlacklist = XPath.newInstance("./@spamblacklist");
          String spamBlacklist = xpaSpamBlacklist.valueOf(node);
          if (spamBlacklist != null) {
            throw new APIException(GT._("URL {0} is blacklisted", spamBlacklist));
          }
          throw new APIException(xmlOutputter.outputString(node));
        }
        XPath xpaWait = XPath.newInstance("./@wait");
        XPath xpaDetails = XPath.newInstance("./@details");
        return QueryResult.createErrorQuery(result, xpaDetails.valueOf(node), xpaWait.valueOf(node));
      }
    } catch (JDOMException e) {
      log.error("Error login", e);
      throw new APIException("Error parsing XML result", e);
    }
    return QueryResult.createErrorQuery(null, null, null);
  }

  /**
   * @param page Page.
   * @param root Root element.
   * @param query XPath query to retrieve the contents 
   * @throws JDOMException
   */
  private boolean constructContents(Page page, Element root, String query)
      throws APIException {
    if (page == null) {
      throw new APIException("Page is null");
    }
    boolean redirect = false;
    try {
      XPath xpaPage = XPath.newInstance(query);
      Element node = (Element) xpaPage.selectSingleNode(root);
      if (node != null) {
        XPath xpaNamespace = XPath.newInstance("./@ns");
        page.setNamespace(xpaNamespace.valueOf(node));
        if (node.getAttribute("redirect") != null) {
          redirect = true;
          page.isRedirect(true);
        }
        if (node.getAttribute("missing") != null) {
          page.setExisting(Boolean.FALSE);
        }
        XPath xpaPageId = XPath.newInstance("./@pageid");
        page.setPageId(xpaPageId.valueOf(node));
        XPath xpaStartTimestamp = XPath.newInstance("./@starttimestamp");
        page.setStartTimestamp(xpaStartTimestamp.valueOf(node));
      }
      XPath xpa = XPath.newInstance(query + "/revisions/rev");
      node = (Element) xpa.selectSingleNode(root);
      if (node != null) {
        XPath xpaContents = XPath.newInstance(".");
        XPath xpaRevision = XPath.newInstance("./@revid");
        XPath xpaTimestamp = XPath.newInstance("./@timestamp");
        page.setContents(xpaContents.valueOf(node));
        page.setExisting(Boolean.TRUE);
        page.setRevisionId(xpaRevision.valueOf(node));
        page.setContentsTimestamp(xpaTimestamp.valueOf(node));
      }
      xpa = XPath.newInstance(query + "/protection/pr[@type=\"edit\"]");
      node = (Element) xpa.selectSingleNode(root);
      if (node != null) {
        XPath xpaLevel = XPath.newInstance("./@level");
        page.setEditProtectionLevel(xpaLevel.valueOf(node));
      }
    } catch (JDOMException e) {
      log.error("Error contents for page " + page.getTitle(), e);
      throw new APIException("Error parsing XML result", e);
    }
    return redirect;
  }

  /**
   * @param pages Pages.
   * @param root Root element.
   * @param query XPath query to retrieve the contents 
   * @throws APIException
   */
  private void constructContents(List<Page> pages, Element root, String query)
      throws APIException {
    if (pages == null) {
      throw new APIException("Pages is null");
    }
    try {
      XPath xpaPage = XPath.newInstance(query);
      XPath xpaTitle = XPath.newInstance("./@title");
      XPath xpaRev = XPath.newInstance("./revisions/rev");
      XPath xpaContents = XPath.newInstance(".");
      List resultPages = xpaPage.selectNodes(root);
      Iterator iterPages = resultPages.iterator();
      while (iterPages.hasNext()) {
        Element currentPage = (Element) iterPages.next();
        String title = xpaTitle.valueOf(currentPage);
        Element currentRev = (Element) xpaRev.selectSingleNode(currentPage);
        String contents = xpaContents.valueOf(currentRev);
        
        for (Page page : pages) {
          if (Page.areSameTitle(page.getTitle(), title)) {
            page.setContents(contents);
          }
        }
      }
    } catch (JDOMException e) {
      log.error("Error contents for pages", e);
      throw new APIException("Error parsing XML result", e);
    }
  }

  /**
   * Update redirect information of a list of pages.
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @param root Root element.
   * @throws APIException
   */
  private void updateRedirectStatus(
      EnumWikipedia wiki,
      List<Page> pages,
      Element root)
      throws APIException {
    try {
      ApiXmlPropertiesResult result = new ApiXmlPropertiesResult(wiki, httpClient);
      result.updateRedirect(root, pages);
    } catch (JDOMException e) {
      log.error("Error redirects", e);
      throw new APIException("Error parsing XML result", e);
    }
  }

  // ==========================================================================
  // API : Authentication
  // ==========================================================================

  /**
   * Login into Wiki.
   * (<code>action=login</code>).
   * 
   * @param wiki Wiki.
   * @param username User name.
   * @param password Password.
   * @param login Flag indicating if login should be done.
   * @return Login status.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Login">API:Login</a>
   */
  @Override
  public LoginResult login(
      EnumWikipedia wiki,
      String username,
      String password,
      boolean login) throws APIException {
    logout(wiki);
    ApiLoginResult result = new ApiXmlLoginResult(wiki, httpClient);
    ApiLoginRequest request = new ApiLoginRequest(wiki, result);
    if (login) {
      return request.login(username, password);
    }
    return LoginResult.createCorrectLogin();
  }

  /**
   * Logout.
   * (<code>action=logout</code>).
   * 
   * @param wiki Wiki.
   * @see <a href="http://www.mediawiki.org/wiki/API:Logout">API:Logout</a>
   */
  @Override
  public void logout(EnumWikipedia wiki) {
    if (!wiki.getConnection().isClean()) {
      wiki.getConnection().clean();
      ApiLogoutResult result = new ApiXmlLogoutResult(wiki, httpClient);
      ApiLogoutRequest request = new ApiLogoutRequest(wiki, result);
      try {
        request.logout();
      } catch (APIException e) {
        // Nothing to do
      }
    }
  }

  /**
   * Retrieve tokens.
   * (<code>action=tokens</code>).
   * 
   * @param wiki Wiki.
   * @throws APIException
   */
  @Override
  public void retrieveTokens(EnumWikipedia wiki) throws APIException {
    ApiTokensResult result = new ApiXmlTokensResult(wiki, httpClient);
    ApiTokensRequest request = new ApiTokensRequest(wiki, result);
    request.retrieveTokens();
  }

  // ==========================================================================
  // API : Queries / Meta information
  // ==========================================================================

  /**
   * Load site information.
   * (<code>action=query</code>, <code>meta=siteinfo</code>).
   * 
   * @param wiki Wiki.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Meta#siteinfo_.2F_si">API:Meta</a>
   */
  private void loadSiteInfo(EnumWikipedia wiki) throws APIException {
    ApiSiteInfoResult result = new ApiXmlSiteInfoResult(wiki, httpClient);
    ApiSiteInfoRequest request = new ApiSiteInfoRequest(wiki, result);
    request.loadSiteInformation(true, true, true, true, true, true);
  }

  // ==========================================================================
  // API : Queries / All messages
  // ==========================================================================

  /**
   * Load messages.
   * (<code>action=query</code>, <code>meta=allmessages</code>).
   * 
   * @param wiki Wiki.
   * @param messageName Message name.
   * @throws APIException
   * @see <a href="https://www.mediawiki.org/wiki/API:Allmessages">API:Allmessages</a>
   */
  @Override
  public String loadMessage(EnumWikipedia wiki, String messageName) throws APIException {
    ApiAllMessagesResult result = new ApiXmlAllMessagesResult(wiki, httpClient);
    ApiAllMessagesRequest request = new ApiAllMessagesRequest(wiki, result);
    return request.loadMessage(messageName);
  }

  // ==========================================================================
  // API : Queries / Properties
  // ==========================================================================

  /**
   * Retrieves the categories of a page.
   * (<code>action=query</code>, <code>prop=categories</code>).
   * 
   * @param wiki Wiki.
   * @param page Page.
   * @throws APIException
   * @see <a href="https://www.mediawiki.org/wiki/API:Categories">API:Categories</a>
   */
  @Override
  public void retrieveCategories(
      EnumWikipedia wiki,
      Page page) throws APIException {
    ApiCategoriesResult result = new ApiXmlCategoriesResult(wiki, httpClient);
    ApiCategoriesRequest request = new ApiCategoriesRequest(wiki, result);
    request.retrieveCategories(page);
  }

  /**
   * Retrieves the informations of a list of pages.
   * (<code>action=query</code>, <code>prop=info</code>).
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#info_.2F_in">API:Properties#info</a>
   */
  @Override
  public void retrieveInfo(
      EnumWikipedia wiki,
      Collection<Page> pages) throws APIException {
    ApiInfoResult result = new ApiXmlInfoResult(wiki, httpClient);
    ApiInfoRequest request = new ApiInfoRequest(wiki, result);
    request.loadInformations(pages);
  }

  /**
   * Retrieves the contents of a list of pages.
   * (<code>action=query</code>, <code>prop=revisions</code>).
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @param usePageId True if page identifiers should be used.
   * @param withRedirects Flag indicating if redirects information should be retrieved.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#revisions_.2F_rv">API:Properties#revisions</a>
   */
  @Override
  public void retrieveContents(
      EnumWikipedia wiki,
      Collection<Page> pages, boolean usePageId,
      boolean withRedirects)
      throws APIException {
    ApiRevisionsResult result = new ApiXmlRevisionsResult(wiki, httpClient);
    ApiRevisionsRequest request = new ApiRevisionsRequest(wiki, result);
    request.loadContent(pages, usePageId, withRedirects);
  }

  /**
   * Retrieves the templates of <code>page</code>.
   * 
   * @param wiki Wiki.
   * @param page The page.
   */
  @Override
  public void retrieveTemplates(EnumWikipedia wiki, Page page)
      throws APIException {
    ApiTemplatesResult result = new ApiXmlTemplatesResult(wiki, httpClient);
    ApiTemplatesRequest request = new ApiTemplatesRequest(wiki, result);
    request.loadTemplates(page);
  }

  /**
   * Initialize the disambiguation flags of a list of <code>pages</code>.
   * (<code>action=query</code>, <code>prop=categories</code>) or
   * (<code>action=query</code>, <code>prop=templates</code>).
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @param forceApiCall True if API call should be forced even if the list of disambiguation pages is loaded.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#categories_.2F_cl">API:Properties#categories</a>
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#templates_.2F_tl">API:Properties#templates</a>
   */
  @Override
  public void initializeDisambiguationStatus(
      EnumWikipedia wiki, List<Page> pages,
      boolean forceApiCall)
      throws APIException {
    if ((pages == null) || (pages.isEmpty())) {
      return;
    }
    if (wiki.isDisambiguationPagesLoaded() && !forceApiCall) {
      for (Page page : pages) {
        page.setDisambiguationPage(wiki.isDisambiguationPage(page));
        if (page.isRedirect()) {
          for (Page page2 : page.getRedirects()) {
            page2.setDisambiguationPage(wiki.isDisambiguationPage(page2));
          }
        }
      }
    } else {
      // Use __DISAMBIG__ magic word if possible
      WPCConfiguration config = wiki.getConfiguration();
      boolean useDisambig = config.getBoolean(
          WPCConfigurationBoolean.DAB_USE_DISAMBIG_MAGIC_WORD);
      if (useDisambig) {
        ApiPagePropsResult result = new ApiXmlPagePropsResult(wiki, httpClient);
        ApiPagePropsRequest request = new ApiPagePropsRequest(wiki, result);
        request.setDisambiguationStatus(pages);
        return;
      }

      // Use categories if possible
      List<Page> dabCategories = wiki.getConfiguration().getDisambiguationCategories();
      if ((dabCategories != null) && (dabCategories.size() > 0)) {
        ApiCategoriesResult result = new ApiXmlCategoriesResult(wiki, httpClient);
        ApiCategoriesRequest request = new ApiCategoriesRequest(wiki, result);
        request.setDisambiguationStatus(pages);
        return;
      }

      // Use templates otherwise
      ApiTemplatesResult result = new ApiXmlTemplatesResult(wiki, httpClient);
      ApiTemplatesRequest request = new ApiTemplatesRequest(wiki, result);
      request.setDisambiguationStatus(pages);
    }
  }

  /**
   * Retrieves internal links of pages.
   * (<code>action=query</code>, <code>prop=links</code>).
   * 
   * @param wiki Wiki.
   * @param pages List of pages.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#links_.2F_pl">API:Properties#links</a>
   */
  @Override
  public void retrieveLinks(EnumWikipedia wiki, Collection<Page> pages)
      throws APIException {
    ApiLinksResult result = new ApiXmlLinksResult(wiki, httpClient);
    ApiLinksRequest request = new ApiLinksRequest(wiki, result);
    request.loadLinks(pages);
  }

  /**
   * Retrieves internal links of one page.
   * (<code>action=query</code>, <code>prop=links</code>).
   * 
   * @param wiki Wiki.
   * @param page Page.
   * @param namespace Restrict the list to a given namespace.
   * @param knownPages Already known pages.
   * @param redirects True if redirects are requested.
   * @param disambigNeeded True if disambiguation information is needed.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#links_.2F_pl">API:Properties#links</a>
   */
  @Override
  public void retrieveLinks(
      EnumWikipedia wiki, Page page, Integer namespace,
      List<Page> knownPages,
      boolean redirects, boolean disambigNeeded)
      throws APIException {
    ApiLinksResult result = new ApiXmlLinksResult(wiki, httpClient);
    ApiLinksRequest request = new ApiLinksRequest(wiki, result);
    boolean useDisambig = wiki.getConfiguration().getBoolean(
        WPCConfigurationBoolean.DAB_USE_DISAMBIG_MAGIC_WORD);
    List<Page> redirections = redirects ? new ArrayList<Page>() : null;
    request.loadLinks(page, namespace, knownPages, redirections, useDisambig);

    // TODO: Better management of redirections (class)
    if ((redirections != null) && !redirections.isEmpty()) {
      initializeDisambiguationStatus(wiki, redirections, true);
      retrieveContentsWithoutRedirects(wiki, redirections);
    }

    // Retrieve disambiguation information if needed
    if (disambigNeeded && !useDisambig) {
      initializeDisambiguationStatus(wiki, page.getLinks(), false);
    }
  }

  /**
   * Retrieve a specific language link in a page.
   * (<code>action=query</code>, <code>prop=langlinks</code>).
   * 
   * @param from Wiki in which the article is.
   * @param to Wiki to which the link is searched.
   * @param title Page title.
   * @return Page title in the destination wiki.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Properties#langlinks_.2F_ll">API:Properties#langlinks</a>
   */
  @Override
  public String getLanguageLink(EnumWikipedia from, EnumWikipedia to, String title)
      throws APIException {
    ApiLanguageLinksResult result = new ApiXmlLanguageLinksResult(from, httpClient);
    ApiLanguageLinksRequest request = new ApiLanguageLinksRequest(from, result);
    return request.getLanguageLink(DataManager.getPage(from, title, null, null, null), to);
  }

  // ==========================================================================
  // API : Queries / Lists
  // ==========================================================================

  /**
   * Retrieves the list of abuse filters.
   * (<code>action=query</code>, <code>list=abusefilters</code>).
   * 
   * @param wiki Wiki.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Abusefilters">API:Abusefilters</a>
   */
  @Override
  public List<AbuseFilter> retrieveAbuseFilters(EnumWikipedia wiki)
      throws APIException {
    ApiAbuseFiltersResult result = new ApiXmlAbuseFiltersResult(wiki, httpClient);
    ApiAbuseFiltersRequest request = new ApiAbuseFiltersRequest(wiki, result);
    return request.loadAbuseFilters();
  }

  /**
   * Retrieves the abuse log for a filter.
   * (<code>action=query</code>, <code>list=abuselog</code>).
   * 
   * @param wiki Wiki.
   * @param filterId Filter identifier.
   * @param maxDuration Maximum number of days.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Abuselog">API:Abuselog</a>
   */
  @Override
  public List<Page> retrieveAbuseLog(
      EnumWikipedia wiki, Integer filterId,
      Integer maxDuration)
      throws APIException {
    ApiAbuseLogResult result = new ApiXmlAbuseLogResult(wiki, httpClient);
    ApiAbuseLogRequest request = new ApiAbuseLogRequest(wiki, result);
    return request.loadAbuseLog(filterId, maxDuration);
  }

  /**
   * Retrieves the back links of <code>page</code> and initialize redirect status.
   * (<code>action=query</code>, <code>list=backlinks</code>).
   * 
   * @param wiki Wiki.
   * @param page The page.
   * @param redirects True if it should also retrieve links through redirects.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Backlinks">API:Backlinks</a>
   */
  @Override
  public void retrieveBackLinks(
      EnumWikipedia wiki, Page page,
      boolean redirects)
      throws APIException {
    ApiBacklinksResult result = new ApiXmlBacklinksResult(wiki, httpClient);
    ApiBacklinksRequest request = new ApiBacklinksRequest(wiki, result);
    request.loadBacklinks(page, redirects);
  }

  /**
   * Retrieves the pages in which <code>page</code> is embedded.
   * (<code>action=query</code>, <code>list=categorymembers</code>).
   * 
   * @param wiki Wiki.
   * @param category Category.
   * @param depth Depth of lookup for sub-categories.
   * @param limit Flag indicating if the number of results should be limited.
   * @param max Absolute maximum number of results
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Categorymembers">API:Categorymembers</a>
   */
  @Override
  public void retrieveCategoryMembers(
      EnumWikipedia wiki, Page category,
      int depth, boolean limit, int max) throws APIException {
    ApiCategoryMembersResult result = new ApiXmlCategoryMembersResult(wiki, httpClient);
    ApiCategoryMembersRequest request = new ApiCategoryMembersRequest(wiki, result);
    request.loadCategoryMembers(category, depth, limit, max);
  }

  /**
   * Retrieves the pages in which <code>page</code> is embedded.
   * (<code>action=query</code>, <code>list=embeddedin</code>).
   * 
   * @param wiki Wiki.
   * @param page Page.
   * @param namespaces Limit to some name spaces.
   * @param limit Flag indicating if the number of results should be limited.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Embeddedin">API:Embeddedin</a>
   */
  @Override
  public void retrieveEmbeddedIn(
      EnumWikipedia wiki, Page page,
      List<Integer> namespaces, boolean limit) throws APIException {
    ApiEmbeddedInResult result = new ApiXmlEmbeddedInResult(wiki, httpClient);
    ApiEmbeddedInRequest request = new ApiEmbeddedInRequest(wiki, result);
    request.loadEmbeddedIn(page, namespaces, limit);
  }

  /**
   * Retrieves the pages which have a given property.
   * (<code>action=query</code>, <code>list=pageswithprop</code>).
   * 
   * @param wiki Wiki.
   * @param property Property name.
   * @param limit Flag indicating if the number of results should be limited.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Pageswithprop">API:Pageswithprop</a>
   */
  @Override
  public List<Page> retrievePagesWithProp(
      EnumWikipedia wiki,
      String property, boolean limit) throws APIException {
    ApiPagesWithPropResult result = new ApiXmlPagesWithPropResult(wiki, httpClient);
    ApiPagesWithPropRequest request = new ApiPagesWithPropRequest(wiki, result);
    return request.loadPagesWithProp(property, limit);
  }

  /**
   * Retrieves the pages which are protected in creation indefinitely.
   * (<code>action=query</code>, <code>list=protectedtitles</code>).
   * 
   * @param wiki Wiki.
   * @param namespaces Limit to some namespaces.
   * @param limit Flag indicating if the number of results should be limited.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Protectedtitles">API:Protectedtitles</a>
   */
  @Override
  public List<Page> getProtectedTitles(
      EnumWikipedia wiki,
      List<Integer> namespaces, boolean limit) throws APIException {
    ApiProtectedTitlesResult result = new ApiXmlProtectedTitlesResult(wiki, httpClient);
    ApiProtectedTitlesRequest request = new ApiProtectedTitlesRequest(wiki, result);
    return request.loadProtectedTitles(namespaces, limit);
  }

  /**
   * Retrieves a special list of pages.
   * (<code>action=query</code>, <code>list=querypage</code>).
   * 
   * @param wiki Wiki.
   * @param query Type of list.
   * @return List of pages depending on the query.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Querypage">API:Querypage</a>
   */
  @Override
  public List<Page> getQueryPages(
      EnumWikipedia wiki, EnumQueryPage query) throws APIException {
    ApiQueryPageResult result = new ApiXmlQueryPageResult(wiki, httpClient);
    ApiQueryPageRequest request = new ApiQueryPageRequest(wiki, result);
    return request.loadQueryPage(query);
  }

  /**
   * Retrieves random pages.
   * (<code>action=query</code>, <code>list=random</code>).
   * 
   * @param wiki Wiki.
   * @param count Number of random pages.
   * @param redirects True if redirect pages are requested.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Random">API:Random</a>
   */
  @Override
  public List<Page> getRandomPages(
      EnumWikipedia wiki, int count,
      boolean redirects) throws APIException {
    ApiRandomPagesResult result = new ApiXmlRandomPagesResult(wiki, httpClient);
    ApiRandomPagesRequest request = new ApiRandomPagesRequest(wiki, result);
    return request.loadRandomList(count, redirects);
  }

  /**
   * Retrieves recent changes.
   * (<code>action=query</code>, <code>list=recentchanges</code>).
   * 
   * @param wiki Wiki.
   * @param start The timestamp to start listing from.
   * @param recentChanges The list of recent changes to be filled.
   * @return The timestamp to use as a starting point for the next call.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Recentchanges">API:Recentchanges</a>
   */
  @Override
  public String getRecentChanges(
      EnumWikipedia wiki,
      String start, List<RecentChange> recentChanges) throws APIException {
    ApiRecentChangesResult result = new ApiXmlRecentChangesResult(wiki, httpClient);
    ApiRecentChangesRequest request = new ApiRecentChangesRequest(wiki, result);
    return request.loadRecentChanges(start, recentChanges);
  }

  /**
   * Retrieves similar pages.
   * (<code>action=query</code>, <code>list=search</code>).
   * 
   * @param wiki Wiki.
   * @param page The page.
   * @param limit Flag indicating if the number of results should be limited.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Search">API:Search</a>
   */
  @Override
  public void retrieveSimilarPages(
      EnumWikipedia wiki, Page page, boolean limit)
      throws APIException {
    ApiSearchResult result = new ApiXmlSearchResult(wiki, httpClient);
    ApiSearchRequest request = new ApiSearchRequest(wiki, result);
    request.searchSimilarPages(page, limit);
  }

  /**
   * Retrieve user information.
   * (<code>action=query</code>, <code>list=users</code>).
   * 
   * @param wiki Wiki.
   * @param name User name.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Users">API:Users</a>
   */
  @Override
  public User retrieveUser(
      EnumWikipedia wiki, String name) throws APIException {
    ApiUsersResult result = new ApiXmlUsersResult(wiki, httpClient);
    ApiUsersRequest request = new ApiUsersRequest(wiki, result);
    return request.retrieveUser(name);
  }

  /**
   * Retrieve raw watch list.
   * (<code>action=query</code>, <code>list=watchlistraw</code>).
   * 
   * @param wiki Wiki.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Watchlistraw">API:Watchlistraw</a>
   */
  @Override
  public List<Page> retrieveRawWatchlist(EnumWikipedia wiki) throws APIException {
    ApiRawWatchlistResult result =
        new ApiXmlRawWatchlistResult(wiki, httpClient);
    ApiRawWatchlistRequest request =
        new ApiRawWatchlistRequest(wiki, result);
    return request.loadWatchlistRaw();
  }

  // ==========================================================================
  // API : Expanding templates and rendering.
  // ==========================================================================

  /**
   * Expand templates in a text.
   * (<code>action=expandtemplates</code>).
   * 
   * @param wiki Wiki.
   * @param title The title to use (for example in {{PAGENAME}}).
   * @param text The text with templates in it.
   * @return Text with templates expanded.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Parsing_wikitext#expandtemplates">API:Parsing wikitext</a>
   */
  @Override
  public String expandTemplates(
      EnumWikipedia wiki, String title, String text) throws APIException {
    ApiExpandResult result = new ApiXmlExpandResult(wiki, httpClient);
    ApiExpandRequest request = new ApiExpandRequest(wiki, result);
    return request.expandTemplates(title, text);
  }

  /**
   * Parse text.
   * (<code>action=parse</code>).
   * 
   * @param wiki Wiki.
   * @param title The title to use (for example in {{PAGENAME}}).
   * @param text The text with templates in it.
   * @param full True to do a full parsing.
   * @return Parsed text.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Parsing_wikitext#parse">API:Parsing wikitext</a>
   */
  @Override
  public String parseText(
      EnumWikipedia wiki, String title, String text, boolean full) throws APIException {
    ApiParseResult result = new ApiXmlParseResult(wiki, httpClient);
    ApiParseRequest request = new ApiParseRequest(wiki, result);
    StringBuilder suffix = new StringBuilder();
    while ((text != null) &&
           (text.length() > 0) &&
           ("\n ".indexOf(text.charAt(text.length() - 1)) >= 0)) {
      suffix.append(text.charAt(text.length() - 1));
      text = text.substring(0, text.length() - 1);
    }
    text = request.parseText(title, text, full);
    return text + suffix.toString();
  }

  /**
   * Retrieve list of sections.
   * (<code>action=parse</code>).
   * 
   * @param wiki Wiki.
   * @param page Page.
   * @return List of sections.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Parsing_wikitext#parse">API:Parsing wikitext</a>
   */
  @Override
  public List<Section> retrieveSections(
      EnumWikipedia wiki, Page page) throws APIException {
    ApiParseResult result = new ApiXmlParseResult(wiki, httpClient);
    ApiParseRequest request = new ApiParseRequest(wiki, result);
    return request.retrieveSections(page);
  }

  // ==========================================================================
  // API : Purging pages' caches.
  // ==========================================================================

  /**
   * Purge the cache of <code>page</code>.
   * (<code>action=purge</code>).
   * 
   * @param wiki Wiki.
   * @param page The page.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Purge">API:Purge</a>
   */
  @Override
  public void purgePageCache(EnumWikipedia wiki, Page page)
      throws APIException {
    ApiPurgeResult result = new ApiXmlPurgeResult(wiki, httpClient);
    ApiPurgeRequest request = new ApiPurgeRequest(wiki, result);
    request.purgePage(page);
  }

  // ==========================================================================
  // API : Changing wiki content / Create and edit pages.
  // ==========================================================================

  /**
   * Delete the <code>page</code>.
   * (<code>action=delete</code>).
   * 
   * @param wiki Wiki.
   * @param page The page.
   * @param reason Reason for deleting the page.
   * @param automatic True if the modification is automatic.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Delete">API:Delete</a>
   */
  @Override
  public void deletePage(
      EnumWikipedia wiki, Page page,
      String reason, boolean automatic)
      throws APIException {
    ApiDeleteResult result = new ApiXmlDeleteResult(wiki, httpClient);
    ApiDeleteRequest request = new ApiDeleteRequest(wiki, result);
    request.deletePage(page, reason, automatic);
  }

  // ==========================================================================
  // API : TemplateData.
  // ==========================================================================

  /**
   * Retrieve the TemplateData for <code>page</code>.
   * (<code>action=templatedata</code>).
   * 
   * @param wiki Wiki.
   * @param page The page.
   * @return TemplateData for the page.
   * @throws APIException
   * @see <a href="http://www.mediawiki.org/wiki/API:Delete">API:Delete</a>
   */
  @Override
  public TemplateData retrieveTemplateData(EnumWikipedia wiki, Page page)
      throws APIException {
    ApiTemplateDataResult result = new ApiJsonTemplateDataResult(wiki, httpClient);
    ApiTemplateDataRequest request = new ApiTemplateDataRequest(wiki, result);
    return request.retrieveTemplateData(page);
  }

  // ==========================================================================
  // Recent changes management.
  // ==========================================================================

  /**
   * Recent changes manager.
   */
  private final Map<EnumWikipedia, RecentChangesManager> rcManagers =
      new Hashtable<EnumWikipedia, RecentChangesManager>();

  /**
   * Adds a <code>RecentChangesListener</code> to the API.
   *
   * @param wiki Wiki.
   * @param listener Recent changes listener.
   */
  @Override
  public void addRecentChangesListener(
      EnumWikipedia wiki,
      RecentChangesListener listener) {
    RecentChangesManager rcManager = rcManagers.get(wiki);
    if (rcManager == null) {
      rcManager = new RecentChangesManager(wiki, this);
      rcManagers.put(wiki, rcManager);
    }
    rcManager.addRecentChangesListener(listener);
  }

  /**
   * Removes a <code>RecentChangesListener</code> from the API.
   * 
   * @param wiki Wiki.
   * @param listener Recent changes listener.
   */
  @Override
  public void removeRecentChangesListener(
      EnumWikipedia wiki,
      RecentChangesListener listener) {
    RecentChangesManager rcManager = rcManagers.get(wiki);
    if (rcManager != null) {
      rcManager.removeRecentChangesListener(listener);
    }
  }

  // ==========================================================================
  // General methods
  // ==========================================================================

  /**
   * Returns an initialized set of properties.
   * 
   * @param action Action called in the MediaWiki API.
   * @param newApi New API (api.php) or not (query.php).
   * @return Properties.
   */
  private Map<String, String> getProperties(
      String action,
      boolean newApi) {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(newApi ? "action" : "what", action);
    properties.put("format", "xml");
    return properties;
  }

  /**
   * Returns the root element of the XML document returned by MediaWiki API.
   * 
   * @param wikipedia Wikipedia.
   * @param properties Properties to drive the API.
   * @param maxTry Maximum number of tries.
   * @return Root element.
   * @throws APIException
   */
  private Element getRoot(
      EnumWikipedia       wikipedia,
      Map<String, String> properties,
      int                 maxTry)
      throws JDOMParseException, APIException {
    Element root = null;
    HttpMethod method = null;
    int attempt = 0;
    for (;;) {
      try {
        attempt++;
        method = createHttpMethod(wikipedia, properties);
        int statusCode = httpClient.executeMethod(method);
        if (statusCode != HttpStatus.SC_OK) {
          String message = "URL access returned " + HttpStatus.getStatusText(statusCode);
          log.error(message);
          if (attempt >= maxTry) {
            log.warn("Error. Maximum attempts count reached.");
            throw new APIException(message, statusCode);
          }
          waitBeforeRetrying();
        } else {
          InputStream stream = method.getResponseBodyAsStream();
          stream = new BufferedInputStream(stream);
          Header contentEncoding = method.getResponseHeader("Content-Encoding");
          if (contentEncoding != null) {
            if (contentEncoding.getValue().equals("gzip")) {
              stream = new GZIPInputStream(stream);
            }
          }
          SAXBuilder sxb = new SAXBuilder();
          Document document = sxb.build(stream);
          traceDocument(document);
          root = document.getRootElement();
          checkForError(root);
          return root;
        }
      } catch (JDOMParseException e) {
        // NOTE: to deal with api.php login action being disabled.
        String message = "JDOMParseException: " + e.getMessage();
        log.error(message);
        if (attempt > maxTry) {
          log.warn("Error. Maximum attempts count reached.");
          throw e;
        }
        waitBeforeRetrying();
      } catch (JDOMException e) {
        String message = "JDOMException: " + e.getMessage();
        log.error(message);
        if (attempt > maxTry) {
          log.warn("Error. Maximum attempts count reached.");
          throw new APIException("Error parsing XML result", e);
        }
        waitBeforeRetrying();
      } catch (IOException e) {
        String message = "" + e.getClass().getName() + ": " + e.getMessage();
        log.error(message);
        if (attempt > maxTry) {
          log.warn("Error. Maximum attempts count reached.");
          throw new APIException("Error accessing MediaWiki", e);
        }
        waitBeforeRetrying();
      } catch (APIException e) {
        if (!e.shouldRetry() || (attempt > e.getMaxRetry())) {
          throw e;
        }
        e.waitForRetry();
      } finally {
        if (method != null) {
          method.releaseConnection();
        }
      }
      log.warn("Error. Trying again");
    }
  }

  /**
   * Wait after a problem occurred.
   */
  private void waitBeforeRetrying() {
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      // Nothing
    }
  }

  /**
   * Check current time to see if edit is authorized (wait if needed).
   * 
   * @param user Current user.
   * @param namespace Name space for the edit.
   */
  private void checkTimeForEdit(User user, Integer namespace) {
    Configuration config = Configuration.getConfiguration();
    int minimumTime = config.getInt(null, ConfigurationValueInteger.TIME_BETWEEN_EDIT);
    int maxEdits = 0;
    if ((namespace == null) || (namespace.intValue() % 2 == 0)) {
      config.getInt(null, ConfigurationValueInteger.MAX_EDITS_PER_MINUTE);
      if ((maxEdits > ConfigurationValueInteger.MAX_EDITS_PER_MINUTE_NORMAL) ||
          (maxEdits <= 0)) {
        if (!user.isMemberOf("admin") &&
            !user.isMemberOf("bot")) {
          maxEdits = ConfigurationValueInteger.MAX_EDITS_PER_MINUTE_NORMAL;
        }
      }
    }
    if ((minimumTime <= 0) && (maxEdits <= 0)) {
      return;
    }
    synchronized (editLock) {
      long currentTime = System.currentTimeMillis();
      if ((minimumTime > 0) && (!lastEditTimes.isEmpty())) {
        long lastEditTime = lastEditTimes.getLast();
        if (currentTime < lastEditTime + minimumTime * 1000) {
          try {
            Thread.sleep(lastEditTime + minimumTime * 1000 - currentTime);
          } catch (InterruptedException e) {
            // Nothing to do
          }
          currentTime = System.currentTimeMillis();
        }
      }
      while ((!lastEditTimes.isEmpty()) &&
             (lastEditTimes.getFirst() + 60 * 1000 <= currentTime)) {
        lastEditTimes.removeFirst();
      }
      if ((maxEdits > 0) && (lastEditTimes.size() >= maxEdits)) {
        try {
          Thread.sleep(lastEditTimes.getFirst() + 60 * 1000 - currentTime);
        } catch (InterruptedException e) {
          // Nothing to do
        }
        currentTime = System.currentTimeMillis();
      }
      lastEditTimes.add(currentTime);
    }
  }

  /**
   * Create an HttpMethod.
   * 
   * @param wikipedia Wikipedia.
   * @param properties Properties to drive the API.
   * @return HttpMethod.
   */
  private HttpMethod createHttpMethod(
      EnumWikipedia       wikipedia,
      Map<String, String> properties) {
    boolean getMethod = canUseGetMethod(properties);
    Configuration config = Configuration.getConfiguration();
    boolean useHttps = !config.getBoolean(null, ConfigurationValueBoolean.FORCE_HTTP_API);
    return HttpUtils.createHttpMethod(
        wikipedia.getSettings().getApiURL(useHttps),
        properties,
        getMethod);
  }

  /**
   * @param properties Properties to drive the API.
   * @return True if GET method can be used.
   */
  private boolean canUseGetMethod(Map<String, String> properties) {
    if (properties == null) {
      return false;
    }
    String action = properties.get("action");
    if (action == null) {
      return false;
    }
    if (ApiRequest.ACTION_QUERY.equals(action)) {
      return true;
    }
    return false;
  }

  /**
   * Check for errors reported by the API.
   * 
   * @param root Document root.
   * @throws APIException
   */
  private void checkForError(Element root) throws APIException {
    if (root == null) {
      return;
    }
    
    // Check for errors
    try {
      XPath xpa = XPath.newInstance("/api/error");
      List listErrors = xpa.selectNodes(root);
      if (listErrors != null) {
        Iterator iterErrors = listErrors.iterator();
        XPath xpaCode = XPath.newInstance("./@code");
        XPath xpaInfo = XPath.newInstance("./@info");
        while (iterErrors.hasNext()) {
          Element currentNode = (Element) iterErrors.next();
          String text = "Error reported: " + xpaCode.valueOf(currentNode) + " - " + xpaInfo.valueOf(currentNode);
          log.warn(text);
          throw new APIException(text, xpaCode.valueOf(currentNode));
        }
      }
    } catch (JDOMException e) {
      log.error("JDOMException: " + e.getMessage());
    }
    
    // Check for warnings
    try {
      XPath xpa = XPath.newInstance("/api/warnings/*");
      List listWarnings = xpa.selectNodes(root);
      if (listWarnings != null) {
        Iterator iterWarnings = listWarnings.iterator();
        while (iterWarnings.hasNext()) {
          Element currentNode = (Element) iterWarnings.next();
          log.warn("Warning reported: " + currentNode.getName() + " - " + currentNode.getValue());
        }
      }
    } catch( JDOMException e) {
      log.error("JDOMException: " + e.getMessage());
    }
  }

  /**
   * Ask for captcha answer.
   * 
   * @param wikipedia Wikipedia.
   * @param captcha Captcha.
   * @return Answer.
   */
  private String getCaptchaAnswer(
      EnumWikipedia wikipedia,
      CaptchaException captcha) {
    // TODO: Move Swing parts out of API
    if (captcha == null) {
      return null;
    }
    if ((captcha.getQuestion() != null) && (captcha.getQuestion().trim().length() > 0)) {
      return Utilities.askForValue(
          null,
          GT._("This action is protected by a CAPTCHA.\nWhat is the answer to the following question ?") + "\n" + captcha.getQuestion(),
          "", null);
    }
    if ((captcha.getURL() != null) && (captcha.getURL().trim().length() > 0)) {
      Utilities.browseURL(wikipedia.getSettings().getHostURL(false) + captcha.getURL());
      return Utilities.askForValue(
          null,
          GT._("This action is protected by a CAPTCHA.\nWhat is the answer to the question displayed in your browser ?"),
          "", null);
    }
    return null;
  }

  /**
   * Trace a document contents.
   * 
   * @param doc Document.
   */
  private void traceDocument(Document doc) {
    if (DEBUG_XML) {
      try {
        System.out.println("********** START OF DOCUMENT **********");
        xmlOutputter.output(doc, System.out);
        System.out.println("**********  END OF DOCUMENT  **********");
      } catch (IOException e) {
        // Nothing to do
      }
    }
  }
}
