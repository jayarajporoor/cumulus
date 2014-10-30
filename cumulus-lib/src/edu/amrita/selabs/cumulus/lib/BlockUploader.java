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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface BlockUploader {
	void putBlock(String bid, String fileid, File blockFile, long blockSize, String encodedPrivateKey, PutLog.PutLogEntry logEntry, PutLog log, File workingFolder) throws IOException;
	void putBlock(String bid, String fileid, File blockFile, long blockSize, String encodedPrivateKey, PutLog.PutLogEntry logEntry, PutLog log, File workingFolder, boolean isInfoBlock) throws IOException;	
}
