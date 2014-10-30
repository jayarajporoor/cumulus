/*
 * This file is part of Cumulus software system developed at SE Labs, Amrita University.
 *
 * Cumulus is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Cumulus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Libav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package edu.amrita.selabs.cumulus.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public interface CumulusClient {
	
	public void setProperties(Properties props);
	
	public PutLog put(String filepath) throws Exception;
	public GetInfo get(String filename, String targetFolder) throws Exception;
	public DeleteInfo delete(String filename) throws Exception;
}

