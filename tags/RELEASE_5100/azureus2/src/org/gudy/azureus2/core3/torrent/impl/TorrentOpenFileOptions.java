/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.gudy.azureus2.core3.torrent.impl;

import java.io.File;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;

/**
 * Class to store the file list of a Torrent.  Used to populate table and
 * store user's choices
 * <P>
 * This was copied out of the UI code, and still contains some crap code
 */
public class TorrentOpenFileOptions
{
	/** relative path + full file name as specified by the torrent */
	/** @todo: getter/setters */
	public final String orgFullName;
	
	/** @todo: getter/setters */
	public final String orgFileName;

	/** @todo: getter/setters */
	public long lSize;

	/** Whether to download this file.  Probably should be switched to the DND state variable */
	private boolean toDownload;

	private String destFileName;
	private String destPathName;

	/** @todo: getter/setters */
	public int iIndex;

	/** @todo: getter/setters */
	public boolean isValid;

	/** @todo: getter/setters */
	public final TorrentOpenOptions parent;
	

	/**
	 * Init
	 * 
	 * @param parent 
	 * @param torrentFile
	 * @param iIndex
	 */
	public TorrentOpenFileOptions(TorrentOpenOptions parent, TOTorrentFile torrentFile,
			int iIndex) {
		this.parent = parent;
		lSize = torrentFile.getLength();
		this.iIndex = iIndex;
		setToDownload(true);
		isValid = true;
		
		orgFullName = torrentFile.getRelativePath(); // translated to locale
		orgFileName = new File(orgFullName).getName();
	}
	
	public void setFullDestName(String newFullName)
	{
		if(newFullName == null)
		{
			setDestPathName(null);
			setDestFileName(null);
			return;
		}
			
		File newPath = new File(newFullName);
		setDestPathName(newPath.getParent());
		setDestFileName(newPath.getName());
	}
	
	public void setDestPathName(String newPath)
	{
		if(parent.getTorrent().isSimpleTorrent())
			parent.setParentDir(newPath);
		else
			destPathName = newPath;
	}
	
	public void setDestFileName (String newFileName)
	{
		if(orgFileName.equals(newFileName))
			destFileName = null;
		else
			destFileName = newFileName;			
	}

	public String getDestPathName() {
		if (destPathName != null)
			return destPathName;

		if (parent.getTorrent().isSimpleTorrent())
			return parent.getParentDir();

		return new File(parent.getDataDir(), orgFullName).getParent();
	}
	
	public String getDestFileName() {
		return destFileName == null ? orgFileName : destFileName;
	}

	public File getDestFileFullName() {
		String path = getDestPathName();
		String file = getDestFileName();
		return new File(path,file);
	}

	public boolean okToDisable() {
		return /* lSize >= MIN_NODOWNLOAD_SIZE	|| */parent.okToDisableAll();
	}

	public File
	getInitialLink()
	{
		return( parent.getInitialLinkage( iIndex ));
	}
	
	public boolean isLinked()
	{
		return destFileName != null || destPathName != null;
	}

	public boolean isToDownload() {
		return toDownload;
	}

	public void setToDownload(boolean toDownload) {
		this.toDownload = toDownload;
		parent.fileDownloadStateChanged(this, toDownload);
	}
}