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

require 'utils.php';

function auth_check_credentials($pdo, $username, $secret)
{
	$select = "SELECT uid, username, salted_secret, salt from users where username=:username";
	
	try
	{
		$st = $pdo->prepare($select);
		$st->execute(array(':username' => $username));
		$res = $st->fetchAll();
	
		foreach($res as $r)
		{
			$raw_secret = utils_hex2bin($secret);
			$raw_salted_secret = hash('SHA256', $raw_secret . $r['salt'], true);
			if(strcmp($r['salted_secret'], $raw_salted_secret) == 0)
			{
				return array($r['uid'], "");
			}
		}
	}catch(PDOException $e)
	{
		return array(false, $e->getMessage());
	}

	return array(false, utils_get_pdo_error_msg($pdo));
}
?>