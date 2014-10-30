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

function create_node($pdo, $uid)
{
	$args = array("nodename", "avail_space", "publickey", "ipaddr", "port");
	$nodename = utils_getparam('nodename');
	$avail_space = utils_getparam('avail_space');
	$publickey = utils_getparam('publickey');
	$ipaddr = utils_getparam('ipaddr');
	$port = utils_getparam('port');

	foreach($args as $arg)
	{
		if(!isset($$arg) || $$arg == false)
		{
			echo utils_wrap_response("error", "param_absent:$arg");
			exit;
		}
	}

	$hex_nodeid = utils_getparam('nodeid');
		
	if($hex_nodeid == false)
	{
		$nodeid = utils_get_random(8);
		$hex_nodeid = bin2hex($nodeid);
	}else
	{
		$nodeid = utils_hex2bin($hex_nodeid);
	}
	
	try
	{
		$query = "select * from nodes where nodename=:nodename";		
		$st = $pdo->prepare($query);
		$st->execute(array(":nodename" => $nodename));
		$res = $st->fetchAll(); 
		if(count($res) >= 1)
		{
			echo utils_wrap_response("error", "node_name_exists");
			exit;
		}
		 
		$query = "insert into nodes(nodename, nodeid, publickey, uid, last_seen, ipaddr, port, avail_space) " 
		         . "values (:nodename, :nodeid, :publickey, :uid, NOW(), :ipaddr, :port, :avail_space)";

		$st = $pdo->prepare($query);
		$st->execute(array(':nodename' => $nodename, ':nodeid' => $nodeid, ':uid' => $uid,
				           ':publickey' => $publickey, ':ipaddr' => $ipaddr, ':port' => $port,
						   ':avail_space' => $avail_space));
		

		$nid = $pdo->lastInsertId('nid');

		$param_pids = utils_getparam('pids');
		
		$param_points = array();
		
		if($param_pids != false)
		{
			$param_pid_array = explode(",", $param_pids);
			for($i=0;$i<10;$i++)
			{
				if($i < count($param_pid_array))
					$param_points[] = utils_hex2bin($param_pid_array[$i]);
			}
		}

		$points = array(10);
				
		for($i=0;$i<10;$i++)
		{	
			$id = ($i < count($param_points)) ? $param_points[$i] : utils_get_random(4);
			$points[$i] = create_node_id($pdo, $nid, $id);
		}

		$res = "<node><id>$hex_nodeid</id>\n<points>\n";

		foreach ($points as $p)
		{
			$res = $res . "<pid>" . bin2hex($p) . "</pid>\n";
		}
		$res = $res . "</points></node>\n";
		echo utils_wrap_response("success", $res);
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");
	}
}

function create_node_id($pdo, $nid, $id)
{
	$ntries = 20;
	$done = false;
	do
	{
		$query = "insert into points(pid, nid) values (:pid, :nid)";
		$st = $pdo->prepare($query);
		try
		{
			$st->execute(array(':pid' => $id, ':nid' => $nid));
			$done = true;
		}catch(PDOException $e)
		{
			utils_error_log(__FILE__, __LINE__, $e->getMessage());
			if($ntries-- <= 0)
				$done = true;
			else
				$id = utils_get_random(4);
		}
	}while(!$done); 
	return $id;
}
?>