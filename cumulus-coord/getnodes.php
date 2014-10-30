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

function getnodes($pdo, $uid)
{
	$args = array("nodename", "blockid", "blocksize");
	
	$last_seen_limit = config_get_value('last-seen-limit');
	$replication_factor = config_get_value('replication-factor');
	
	$nodename = utils_getparam('nodename');
	$blockid = utils_getparam('blockid');
	$blocksize = utils_getparam('blocksize');
	
	foreach($args as $arg)
	{
		if(!isset($$arg) || $$arg == false)
		{
			echo utils_wrap_response("error", "param_absent:$arg");
			exit;
		}
	}
	
	$need = utils_getparam('need');
	
	if($need == false || $need > $replication_factor)
	{
		$need = $replication_factor;
	}
	
	$dontuse = utils_getparam('dontuse');
		
	if($dontuse === false)
	{
		$dontuse = array();
	}else
	{
		$dontuse = explode(",", $dontuse);
	}
	
	$raw_blockid = utils_hex2bin($blockid);
	
	$raw_short_blockid = substr($raw_blockid, -4, 4);
	
	try
	{

		$query = "select nodes.nodename,nodes.nodeid, nodes.avail_space, nodes.ipaddr, nodes.port, nodes.publickey,".
		         " points.nid, points.pid from nodes,points".
	    	     " where points.nid=nodes.nid and (NOT nodes.nodename =:nodename) ".
	        	 " and points.pid >= :short_blockid and CAST(nodes.avail_space AS DECIMAL(32,0)) >= CAST(:blocksize AS DECIMAL(32,0))".
	         	" and TIMESTAMPDIFF(SECOND, nodes.last_seen, NOW()) <= :last_seen_limit ORDER BY points.pid;";
	
		$st = $pdo->prepare($query);
	
		$st->execute(array(':nodename' => $nodename, ':short_blockid' => $raw_short_blockid, 
			           ':blocksize' => $blocksize, ':last_seen_limit' => $last_seen_limit));		
		
		$nodes = array();
		$result_values = array();

		while(($r = $st->fetch(PDO::FETCH_ASSOC)) != false)
		{
			$hex_nodeid = bin2hex($r['nodeid']);
			if(!in_array($r['nodeid'],$nodes) && !in_array($hex_nodeid,$dontuse))
			{
				$nodes[] = $r['nodeid'];
				$result_values[] = "<node>" . "<id>" 		. $hex_nodeid			. '</id>' 
						                    . "<ip>" 		. $r['ipaddr']          . '</ip>' 
						                    . "<port>" 		. $r['port']          	. '</port>'						                    
				                            . "<publickey>" . $r['publickey']       . '</publickey>' 
				                 . "</node>\n";					
				if(count($nodes) >= $need)
					break;
			}
		}
	
		if(count($nodes) < $need)
		{
			$query = "select nodes.nodename,nodes.nodeid, nodes.avail_space, nodes.ipaddr, nodes.port, nodes.publickey, ".
			        " points.nid, points.pid from nodes,points".
					" where points.nid=nodes.nid and (NOT nodes.nodename =:nodename) ".
					" and CAST(nodes.avail_space AS DECIMAL(32,0)) >= CAST(:blocksize AS DECIMAL(32,0))".
					" and TIMESTAMPDIFF(SECOND, nodes.last_seen, NOW()) <= :last_seen_limit ORDER BY points.pid;";

			$st = $pdo->prepare($query);
		
			$st->execute(array(':nodename' => $nodename, 
					           ':blocksize' => $blocksize, ':last_seen_limit' => $last_seen_limit));
		
					
			while(($r = $st->fetch(PDO::FETCH_ASSOC)) != false)
			{
				$hex_nodeid = bin2hex($r['nodeid']);				
				if(!in_array($r['nodeid'],$nodes) && !in_array($hex_nodeid,$dontuse))
				{
					$nodes[] = $r['nodeid'];
					$result_values[] = "<node>" . "<id>" 		. $hex_nodeid			. '</id>' 
							                    . "<ip>" 		. $r['ipaddr']          . '</ip>' 
							                    . "<port>" 		. $r['port']          	. '</port>'							                    		
					                            . "<publickey>" . $r['publickey']       . '</publickey>' 
					                 . "</node>\n";					
					if(count($nodes) >= $need)
						break;				
				}
			}		
		}
	
		echo utils_wrap_response("success", implode("\n", $result_values));
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");
	}
}

?>