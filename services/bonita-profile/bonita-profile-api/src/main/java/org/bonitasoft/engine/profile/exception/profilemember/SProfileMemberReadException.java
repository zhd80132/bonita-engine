/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.profile.exception.profilemember;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;

/**
 * @author Elias Ricken de Medeiros
 */
public class SProfileMemberReadException extends SBonitaException {

    private static final long serialVersionUID = 799472045606191342L;

    public SProfileMemberReadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SProfileMemberReadException(final Throwable cause) {
        super(cause);
    }

}
