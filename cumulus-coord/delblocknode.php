<?php
function delblocknode($pdo, $uid)
{
	$args = array('blockid', 'type', 'nodeid');

	$blockid = utils_getparam('blockid');
	$nodeid =  utils_getparam('nodeid');
	$type    = utils_getparam('type');
	
	foreach($args as $arg)
	{
		if(!isset($$arg) || $$arg == false)
		{
			echo utils_wrap_response("error", "param_absent:$arg");
			exit;
		}
	}
	
	try
	{
		$pdo->beginTransaction();
		$query = "select nid, avail_space from nodes where nodeid=:nodeid";
		$st = $pdo->prepare($query);
		$st->execute(array(':nodeid' => utils_hex2bin($nodeid)));
		
		if(($r = $st->fetch(Pdo::FETCH_ASSOC)) == false)
		{
			$pdo->abort();
			echo utils_wrap_response("error", "node_not_found");
			exit;
		}
		$nid = $r['nid'];
		$avail_space = $r['avail_space'];

		if($type == "iblock")
			$query = "select blocksize from iblocks where nid=:nid and blockid=:blockid for update";
		else
			$query = "select blocksize from blocks where nid=:nid and blockid=:blockid for update";
		$st = $pdo->prepare($query);
		$st->execute(array(':nid' => $nid, ':blockid' => utils_hex2bin($blockid)));

		if(($r = $st->fetch(Pdo::FETCH_ASSOC)) == false)
		{
			$pdo->rollBack();
			echo utils_wrap_response("error", "block_not_found");
			exit;
		}
		
		$blocksize = $r['blocksize'];
		
		if($type == "iblock")
			$query = "delete from iblocks where nid=:nid and blockid=:blockid";
		else	
			$query = "delete from blocks where nid=:nid and blockid=:blockid";
		$st = $pdo->prepare($query);
		$st->execute(array(':nid' => $nid, ':blockid' => utils_hex2bin($blockid)));
		
		$avail_space = bcadd($avail_space, $blocksize);
		$query = "update nodes set avail_space=:avail_space where nid=:nid";
		$st = $pdo->prepare($query);
		$st->execute(array(':nid' => $nid, ':avail_space' => $avail_space));
		$pdo->commit();
		echo utils_wrap_response("success", "deleted");
		exit;			
	}catch(PDOException $e)
	{
		$pdo->rollBack();
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");		
	}
		
}
?>