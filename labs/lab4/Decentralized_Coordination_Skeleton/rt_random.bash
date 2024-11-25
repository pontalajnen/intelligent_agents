
if [ "$#" -ne 1 ]; then
    echo "No tournament name argument"
    exit 1
fi

cd "tournament" && rm -rf *
cd ..

mv "out/artifacts/Decentralized_Coordination_Skeleton_jar/Decentralized_Coordination_Skeleton.jar" "agents/"
cd "agents"
rm "our_agent.jar"
mv "Decentralized_Coordination_Skeleton.jar" "our_agent.jar"
cd ..

# Create the tournament
java -jar ./logist/logist.jar -new $1 ./agents

# Remove the weird agent2 thing

cd "tournament"
cd $1
sed -i.bak -e "19,22d" agents.xml
rm agents.xml.bak
sed -i.bak -e "7,10d" agents.xml
rm agents.xml.bak
cd ..
cd ..

java -jar ./logist/logist.jar -run $1 ./config/auction.xml

java -jar ./logist/logist.jar -score $1 scores.txt
