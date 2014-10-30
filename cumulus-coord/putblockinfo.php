<?php

function putblockinfo($pdo, $uid)
{
	$args = array('blockid', 'type', 'nodeid', 'blocksize');
	$blockid = utils_getparam('blockid');
	$type    = utils_getparam('type');
	$nodeid = utils_getparam('nodeid');
	$blocksize = utils_getparam('blocksize');
	
	foreach($args as $arg)
	{
		if(!isset($$arg) || $$arg == false)
		{
			echo utils_wrap_response("error", "param_absent:$arg");
			exit;
		}
	}
	
	if($type != "iblock" && $type != "block")
	{
		echo utils_wrap_response("error", "invalid_type");
		exit;
	}
	
	$nodeid = utils_hex2bin($nodeid);
	$blockid = utils_hex2bin($blockid);
	
	try 
	{
		$pdo->beginTransaction();
		$query = "select nid, avail_space from nodes where nodeid=:nodeid for update";
		$st = $pdo->prepare($query);
		$st->execute(array(":nodeid" => $nodeid));
		$r = $st->fetch(Pdo::FETCH_ASSOC);
		if($r != false)
		{
			$nid = $r['nid'];
			$avail_space = $r['avail_space'];
			if($type == "iblock")
				$query = "insert into iblocks(blockid,nid, blocksize) values(:blockid,:nid, :blocksize)";
			else
				$query = "insert into  blocks(blockid,nid, blocksize) values(:blockid,:nid, :blocksize)";
			$st = $pdo->prepare($query);
			try
			{
				$st->execute(array(':blockid' => $blockid, ':nid' => $nid, ':blocksize' => $blocksize));
				$query = "update nodes set avail_space=:avail_space where nid=:nid";
				$st = $pdo->prepare($query);
				$avail_space = bcsub($avail_space, $blocksize);				
				$st->execute(array(':nid' => $nid, ':avail_space' => $avail_space));
				$pdo->commit();
				echo utils_wrap_response("success", "");
			}catch(PDOException $e)
			{
				$duplicate_entry_error = 23000;
				if($st->errorCode() == $duplicate_entry_error)
				{
					echo utils_wrap_response("success", "entry_already_exists");
				}else
				{
					utils_error_log(__FILE__, __LINE__, $e->getMessage());
					echo utils_wrap_response("error", "query_failure");
					$pdo->rollBack();
				}
			}
		}else
		{
			echo utils_wrap_response("error", "node_doesnot_exist");
			$pdo->rollBack();			
		}
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");
		$pdo->rollBack();
	}
	
}
?>