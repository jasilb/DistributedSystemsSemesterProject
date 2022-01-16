#!/bin/bash
mvn test | tee output.log 

mvn compile


cd target/classes

timestamp=$(date +"%Y-%m-%d-%H_%M")

(java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 1 ) | tee -a output.log &
s1=$!
( java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 5 )  | tee -a output.log &
s2=$!
( java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 9) |  tee -a output.log &
s3=$!
(java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 13 ) |  tee -a output.log &
s4=$!
(java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 17 ) |  tee -a output.log &
s5=$!
(java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 21) |  tee -a output.log &
s6=$!
(java "edu.yu.cs.com3800.stage5.ZookeeperStarter" 25) |  tee -a output.log &
s7=$!
(java "edu.yu.cs.com3800.stage5.GatewayStarter" 0 ) | tee -a output.log &
g=$!



(java "edu.yu.cs.com3800.ClientRunner")  |  tee -a output.log &
c1=$!

wait $c1
for i in {1..9}; do
    echo $i
	(java "edu.yu.cs.com3800.ClientRunner" $i) |  tee -a output.log
    
done



echo "kill follower 1 " |  tee -a output.log
kill -9 $s1
sleep 40
(java "edu.yu.cs.com3800.ClientRunner") |  tee -a output.log &
c2=$!

wait $c2
echo "kill leader 25 " |  tee -a output.log
kill -9 $s7
sleep 1
work=()
for i in {10..19}; do
    echo $i
	(java "edu.yu.cs.com3800.ClientRunner" $i)  |  tee -a output.log &
    c=$!
    work+=($c)
    sleep .5
    
done
for j in ${work[@]}; do

    wait $j
done


java "edu.yu.cs.com3800.ClientRunner" 20 | tee -a output.log
l=$!
wait $l


echo "target/classes/$timestamp/gossiper 0.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 1.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 5.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 9.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 13.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 17.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 21.log" tee -a output.log
echo "target/classes/$timestamp/gossiper 25.log" tee -a output.log


kill -9 $s2
kill -9 $s3
kill -9 $s4
kill -9 $s5
kill -9 $s6


