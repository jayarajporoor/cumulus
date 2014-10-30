<?php

function getblockinfo($pdo, $uid)
{
	$args = array('blockid', 'type');
	$blockid = utils_getparam('blockid');
	$type    = utils_getparam('type');
	
	foreach($args as $arg)
	{
		if(!isset($$arg) || $$arg == false)
		{
			echo utils_wrap_response("error", "param_absent:$arg");
			exit;
		}
	}
	
	$aliveonly = utils_getparam('aliveonly');
	if($aliveonly == "true")
	{
		$aliveonly = true;
	}else
	{
		$aliveonly = false;
	}
	
	if($type != "iblock" && $type != "block")
	{
		echo utils_wrap_response("error", "invalid_type");
		exit;
	}
	
	$last_seen_limit = config_get_value('last-seen-limit');
		
	$blockid = utils_hex2bin($blockid);
	
	try
	{
		if($type == "iblock")
			$query = "select nid from iblocks where blockid=:blockid";
		else 
			$query = "select nid from blocks where blockid=:blockid";
		$st0 = $pdo->prepare($query);
		$st0->execute(array(':blockid'=>$blockid));
		$r0 = $st0->fetch(Pdo::FETCH_ASSOC);
		$blockfound = false;
		$nodefound = false;
		$resp = "";
		while($r0 != null)
		{
			$blockfound = true;
			$nid = $r0['nid'];
			if($aliveonly)
			{
				$query = "select nodeid, ipaddr, port, publickey from nodes where nid=:nid " . 
						   "and TIMESTAMPDIFF(SECOND, last_seen, NOW()) <= :last_seen_limit";
				$values = array(':nid' => $nid, ':last_seen_limit' => $last_seen_limit);
			}
			else 
			{
				$query = "select nodeid, ipaddr, port, publickey from nodes where nid=:nid";
				$values = array(':nid' => $nid);
			}
			$st = $pdo->prepare($query);
			$st->execute($values);
			$r = $st->fetch(Pdo::FETCH_ASSOC);
			if($r != false)
			{
				$nodefound=true;
				$nodeid = bin2hex($r['nodeid']);
				$ipaddr = $r['ipaddr'];
				$port = $r['port'];
				$publickey = bin2hex($r['publickey']);
				$resp = $resp . "<node><id>$nodeid</id><ip>$ipaddr</ip><port>$port</port><publickey>$publickey</publickey></node>\n"; 
			}else
			{
				$hexblockid = bin2hex($blockid);
				utils_error_log(__FILE__, __LINE__, "Row for nid=$nid not found in nodes table but exists in blocks table (blockid: $hexblockid)");				
			}
			$r0 = $st0->fetch(Pdo::FETCH_ASSOC);
		}
		if(!$blockfound)
		{
			echo utils_wrap_response("error", "block_not_found");
		}else
		if(!$nodefound)
		{
			echo utils_wrap_response("error", "node_not_found");				
		}else
		{
			echo utils_wrap_response("success", $resp);
		}
		
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");		
	}
	
}
?>