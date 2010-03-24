/*
 *  WikipediaCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2007  Nicolas Vervelle
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipediacleaner.api.data;

import java.util.List;


/**
 * Informations about backlinks.
 */
public class Namespace implements Comparable<Namespace> {

  public final static int MAIN = 0;
  public final static int MAIN_TALK = 1;
  public final static int USER = 2;
  public final static int USER_TALK = 3;
  public final static int WIKIPEDIA = 4;
  public final static int WIKIPEDIA_TALK = 5;
  public final static int IMAGE = 6;
  public final static int IMAGE_TALK = 7;
  public final static int MEDIAWIKI = 8;
  public final static int MEDIAWIKI_TALK = 9;
  public final static int TEMPLATE = 10;
  public final static int TEMPLATE_TALK = 11;
  public final static int HELP = 12;
  public final static int HELP_TALK = 13;
  public final static int CATEGORY = 14;
  public final static int CATEGORY_TALK = 15;

  private Integer id;
  private String title;

  /**
   * @param id Namespace Id.
   * @param title Namespace title.
   */
  public Namespace(String id, String title) {
    try {
      this.id = Integer.parseInt(id);
    } catch (NumberFormatException e) {
      this.id = Integer.valueOf(-1);
    }
    this.title = title;
  }

  /**
   * @param id Namespace id.
   * @param namespaces List of namespaces.
   * @return Matching namespace.
   */
  public static Namespace getNamespace(int id, List<Namespace> namespaces) {
    if (namespaces == null) {
      return null;
    }
    for (Namespace n : namespaces) {
      if ((n != null) && (n.getId() != null) && (id == n.getId().intValue())) {
        return n;
      }
    }
    return null;
  }

  /**
   * Construct a full title name.
   * 
   * @param namespaceId Namespace identifier.
   * @param namespaces List of namespaces.
   * @param title Title (without namespace).
   * @return Fully qualified title.
   */
  public static String getTitle(int namespaceId, List<Namespace> namespaces, String title) {
    if (namespaceId == MAIN) {
      return title;
    }
    Namespace namespace = getNamespace(namespaceId, namespaces);
    if (namespace != null) {
      return namespace.getTitle() + ":" + title;
    }
    return title;
  }

  /**
   * @return Namespace id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * @return Title.
   */
  public String getTitle() {
    return title;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "" + id + ":" + title;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Namespace bl) {
    int compare;

    // Namespace
    compare = id.compareTo(bl.id);
    if (compare != 0) {
      return compare;
    }

    // Title
    compare = title.compareTo(bl.title);
    
    return compare;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (o.getClass() != getClass())) {
      return false;
    }
    Namespace bl = (Namespace) o;
    boolean equals = true;
    equals &= id.equals(bl.id);
    equals &= title.equals(bl.title);
    return equals;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + id.hashCode();
    hash = 31 * hash + title.hashCode();
    return hash;
  }
}
