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


function utils_hex2bin($hexString)
{
	$hexLenght = strlen($hexString);
	// only hex numbers is allowed
	if ($hexLenght % 2 != 0 || preg_match("/[^\da-fA-F]/",$hexString)) return FALSE;
    $binString ="";
	for ($x = 1; $x <= $hexLenght/2; $x++)
	{
		$binString .= chr(hexdec(substr($hexString,2 * $x - 2,2)));
	}
    return $binString;
}

function utils_get_random($n)
{
	$rfile = "/dev/urandom";
	$f = fopen($rfile, "rb");
	$random = fread($f, $n);
	fclose($f);
	return $random;
}

function utils_get_pdo_error_msg($pdo)
{
	$err = $pdo->errorInfo();
	return $err[2];
}

function utils_getparam($name)
{
	if(isset($_POST[$name]))
		return $_POST[$name];
	else
	if(isset($_GET[$name]))
		return $_GET[$name];
	else
		return false;
}

function utils_error_log($file, $line, $msg)
{
	error_log($file . ":" . $line . ":" . $msg);
}

function utils_notice_log($file, $line, $msg)
{
	syslog(LOG_NOTICE, $file . ":" . $line . ":" . $msg);
}


function utils_wrap_response($status, $content)
{
	$ver = config_get_version();
	return "<response status=\"$status\" version=\"$ver\" >" .
			$content .
			"</response>\n";
			 
}
?>