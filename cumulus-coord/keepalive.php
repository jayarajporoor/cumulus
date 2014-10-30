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


function keepalive($pdo, $uid)
{
	$nodename = utils_getparam('nodename');
	
	if($nodename == false)
	{
		echo utils_wrap_response("error", "param_absent:nodename");
		exit;
	}
	
	$avail_space = utils_getparam('avail_space'); 

	try
	{
		if($avail_space != false)
 		{
 			$query = "update nodes set last_seen=NOW(), avail_space=:avail_space where nodename=:nodename and uid=:uid";
 			$st = $pdo->prepare($query);
 			$st->execute(array(':avail_space' => $avail_space, ':nodename' => $nodename, ':uid' => $uid));	
 		}else
 		{
 			$query = "update nodes set last_seen=NOW() where nodename=:nodename and uid=:uid";
 			$st = $pdo->prepare($query);
 			$st->execute(array(':nodename' => $nodename, ':uid' => $uid)); 			
 		}
 		echo utils_wrap_response("success", "");
 	}catch(PDOException $e)
 	{
 		error_log(__FILE__ . ":" .  __LINE__ . ":" . $e->getMessage());
 		echo utils_wrap_response("error", "node_auth");
 	}
}

?>