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

function getpubkey($pdo, $uid)
{
	$nodeid = utils_getparam('nodeid');
	if($nodeid == false)
	{
		echo utils_wrap_response("error", "param_absent:nodeid");
		exit;		
	}

	$nodeid = utils_hex2bin($nodeid);
	try
	{
		$query = "select publickey from nodes where nodeid=:nodeid";
		$st = $pdo->prepare($query);
		$st->execute(array(':nodeid' => $nodeid));
		$res = $st->fetchAll();
		foreach($res as $r)
		{
			echo $r['publickey'] . "\n";
			exit;
		}
		echo "error_key_not_found\n";		
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo "error_db_failure\n";
	}	
}
?>