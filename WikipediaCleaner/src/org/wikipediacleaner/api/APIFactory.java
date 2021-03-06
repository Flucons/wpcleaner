/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2013  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */

package org.wikipediacleaner.api;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.wikipediacleaner.api.check.CheckWiki;
import org.wikipediacleaner.api.impl.MediaWikiAPI;


/**
 * Factory for API access. 
 */
public class APIFactory {

  /**
   * MediaWiki API.
   */
  private static API api;

  /**
   * Check Wiki project.
   */
  private static CheckWiki checkWiki;

  // Initialize static members
  static {

    // Initialize MediaWiki API
    HttpConnectionManager connectionManger = new MultiThreadedHttpConnectionManager();
    HttpClient httpClient = createHttpClient(connectionManger);
    httpClient.getParams().setParameter("http.protocol.single-cookie-header", Boolean.TRUE);
    api = new MediaWikiAPI(httpClient);

    // Initialize WMF Labs access
    connectionManger = new MultiThreadedHttpConnectionManager();
    httpClient = createHttpClient(connectionManger);
    HttpServer labs = new HttpServer(httpClient, "http://tools.wmflabs.org/");

    // Initialize Check Wiki project
    checkWiki = new CheckWiki(labs);
  }

  /**
   * @return MediaWiki API implementation.
   */
  public static API getAPI() {
    return api;
  }

  /**
   * @return Access to Check Wiki project.
   */
  public static CheckWiki getCheckWiki() {
    return checkWiki;
  }

  /**
   * Create an HTTP connection.
   * 
   * @return A HTTP connection.
   */
  private static HttpClient createHttpClient(HttpConnectionManager manager) {
    HttpClient client = new HttpClient(manager);
    client.getParams().setParameter(
        HttpMethodParams.USER_AGENT,
        "WPCleaner (+http://en.wikipedia.org/wiki/User:NicoV/Wikipedia_Cleaner/Documentation)");
    return client;
  }
}
