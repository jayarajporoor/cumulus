<?php

function getnodeinfo($pdo, $uid)
{
	$nodeid = utils_getparam('nodeid');
	if($nodeid == false)
	{
		echo utils_wrap_response("error", "param_absent:nodeid");
		exit;		
	}
	
	try
	{
		$query = "select nodename, ipaddr, port, publickey from nodes where  nodeid=:nodeid";
		
		$st = $pdo->prepare($query);
		
		$st->execute(array(':nodeid' => utils_hex2bin($nodeid)));		
			
		if(($r = $st->fetch(PDO::FETCH_ASSOC)) != false)
		{
			$result	 = 	"<node>" . "<id>" 		 . $nodeid 			. '</id>'
								 . "<ip>" 		 . $r['ipaddr']     . '</ip>'
								 . "<port>" 	 . $r['port']       . '</port>'
								 . "<publickey>" . $r['publickey']  . '</publickey>'
								 . "</node>\n";
			echo utils_wrap_response("success", $result);
		}else
		{
			echo utils_wrap_response("error", "node_not_found");
		}
		
		
	}catch(PDOException $e)
	{
		utils_error_log(__FILE__, __LINE__, $e->getMessage());
		echo utils_wrap_response("error", "query_failure");
	}
}
?>