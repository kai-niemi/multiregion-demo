#!/bin/bash
# Multi-region cloud deployment script using roachprod.

releaseversion="v26.1.3"
nodes=12
dbnodes="1-9"
clientnodes="10,11,12"
regions="eu-central-1,us-east-1,ap-northeast-1"
zones="\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
us-east-1a,\
us-east-1b,\
us-east-1c,\
ap-northeast-1a,\
ap-northeast-1c,\
ap-northeast-1d,\
eu-central-1a,\
us-east-1a,\
ap-northeast-1a"

# https://aws.amazon.com/ec2/instance-types/m6i/
machinetype="m6i.xlarge"

#############################################

if [ "$(whoami)" == "root" ]; then
    fn_echo_warning "Do NOT run as root!"
    exit 1
fi

if [ -z "${CLUSTER}" ]; then
  fn_echo_warning "No \$CLUSTER id variable set!"
  echo "Use: export CLUSTER='your-cluster-id'"
  exit 1
fi

IFS=',' read -ra regions_arr <<< "$regions"
IFS=',' read -ra clients_arr <<< "$clientnodes"

echo "Machines: ${machinetype}"
echo "Nodes: ${nodes}"
echo "DB nodes: ${dbnodes}"
echo "Client nodes: ${clientnodes}"
echo "Regions: ${regions}"
echo "Zones: ${zones}"

#echo "Clients:"
#for value in "${clients_arr[@]}" ; do
#echo -e "$value"
#done
#
#echo "Regions:"
#for value in "${regions_arr[@]}" ; do
#echo -e "$value"
#done

fn_prompt_yes_no(){
	local prompt="$1"

	while true; do
	  echo -e "${prompt}"
		select yn in "Yes" "No" "Quit"; do
        case $yn in
            Yes ) return 0 ;;
            No ) return 1 ;;
            Quit ) exit 0 ;;
        *) echo -e "Please answer yes, no or quit." ;;
        esac
	  done
	done
}

fn_create_cluster(){
  echo ">> Creating cluster"

  roachprod create $CLUSTER --clouds=aws \
  --aws-machine-type-ssd=${machinetype} --aws-zones=${zones} \
  --aws-profile crl-revenue --aws-config ~/rev.json \
  --geo --local-ssd-no-ext4-barrier \
  --nodes=${nodes} \
  --os-volume-size 750 \
  --lifetime 36h0m0s
}
fn_stage_cluster(){
  echo ">> Staging cluster ($releaseversion)"

  roachprod stage $CLUSTER release $releaseversion
}
fn_start_cluster(){
  echo ">> Starting cluster"

  roachprod start --insecure $CLUSTER:$dbnodes
  roachprod admin --insecure --open --ips $CLUSTER:1
}

fn_stage_clients(){
  echo ">> Staging clients"

  roachprod run --insecure ${CLUSTER}:$clientnodes 'sudo apt-get -qq update'
  roachprod run --insecure ${CLUSTER}:$clientnodes 'sudo apt-get -qq install -y openjdk-21-jre-headless htop dstat haproxy'
}

fn_stage_app(){
  echo ">> Staging app"

  roachprod put ${CLUSTER}:$clientnodes run.sh
  roachprod put ${CLUSTER}:$clientnodes target/demo.jar
}

fn_start_haproxy(){
  echo ">> Starting haproxy"

  i=0;
  for c in "${clients_arr[@]}" ; do
    region=${regions_arr[$i]}
    i=($i+1)
    roachprod run ${CLUSTER}:$c "./cockroach gen haproxy --insecure --host $(roachprod ip $CLUSTER:1 --external) --locality=region=$region"
  done

  roachprod run --insecure ${CLUSTER}:$clientnodes 'nohup haproxy -f haproxy.cfg > /dev/null 2>&1 &'
}

if ! fn_prompt_yes_no "Create this cluster?" ; then
  exit 0
fi

fn_create_cluster
fn_stage_cluster
fn_start_cluster
fn_start_haproxy
#fn_stage_app

echo "Done"