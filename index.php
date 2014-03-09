<html>
    <head>
    <title>Gebannte User</title>
    <style>
    table,th,td {
        border:1px solid black;
        border-collapse:collapse
    }
    th,td {
        padding:15px;
    }
    </style>
    </head>
    <body>
        <?php
        $host = "localhost";
        $user = "root";
        $password = "";
        $database = "banplugin";
        $con=mysql_connect($host,$user,$password);

        @mysql_select_db($database) or die("<b>Ein Fehler ist aufgetreten.</b>");

        $query = "SELECT * FROM players";
        $result = mysql_query($query);

        $num = mysql_num_rows($result);

        echo "<b><h1><center>Banned players </center></b></h1><br>";

        if ($num == null || $num < 1)
        {
            echo "<b>Keine gebannten Spieler gefunden.</b>";
            exit;
        }

        mysql_close();

        $i=0;
        echo "<table>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Grund</th>
                  <th>Ban Datum</th>
                  <th>Unban Datum</th>
                  <th>Gebannt von</th>
                </tr>";
        while ($i < $num) {
            echo "<tr>";
            $id=mysql_result($result,$i,"id");
            $name=mysql_result($result,$i,"name");
            $reason=mysql_result($result,$i,"reason");
            $bantimestamp = mysql_result($result,$i,"bantimestamp");
            $unbantimestamp = mysql_result($result,$i,"unbantimestamp");
            $bannedby = mysql_result($result,$i,"bannedby");



            if ($ip == null){
                $ip = "Unknown";
            }
            echo "<td>$id</td>";
            echo "<td>$name</td>";
            echo "<td>$reason</td>";
            echo "<td>$bantimestamp</td>";
            echo "<td>$unbantimestamp</td>";
            echo "<td>$bannedby</td>";
            echo "</tr>";
            $i++;
        }
        echo "</table>";

        echo "<br/><b>Anzahl der gebannten User: $num</b>";
        ?>
    </body>
</html>