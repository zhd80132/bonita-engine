/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.identity.model;

import org.bonitasoft.engine.persistence.PersistentObject;

/**
 * All elements that have name label and description
 * 
 * @author Baptiste Mesta
 */
public interface SNamedElement extends PersistentObject {

    /**
     * Gets the name of the element.
     * 
     * @return the element name
     */
    String getName();

    /**
     * Gets the label of the element.
     * 
     * @return the label name.
     */
    String getDisplayName();

    /**
     * Obtains the description of the element
     * 
     * @return the element description
     */
    String getDescription();

}
