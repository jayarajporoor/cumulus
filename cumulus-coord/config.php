<?php
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

function config_get_version()
{
	return "0.01";
}

function config_get_db()
{
	$dsn = "mysql:host=localhost;port=3306;dbname=cumulus";
	$dbuser="root";
	$dbpassword = "root";
	$dboptions = array();
	$pdo = new PDO($dsn, $dbuser, $dbpassword, $dboptions);
	$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);	
	return $pdo;	
}

$params = array("replication-factor" 		=> 3, 
				//"last-seen-limit" 			=> 60*60*24*30
				"last-seen-limit" 			=> 60*3
		);

function config_get_value($param)
{
	global $params;
	if(!array_key_exists($param, $params))
	{
		utils_error_log(__FILE__, __LINE__, "Param $param absent in the configured param list");
		return false;
	}
	return $params[$param];
}
?>