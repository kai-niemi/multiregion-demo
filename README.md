[![Java CI with Maven](https://github.com/kai-niemi/multiregion-demo/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kai-niemi/multiregion-demo/actions/workflows/maven.yml)

<!-- TOC -->
* [CockroachDB multi-region demo](#cockroachdb-multi-region-demo)
  * [How to run the demo](#how-to-run-the-demo)
  * [Remarks](#remarks)
* [Demo app](#demo-app)
  * [Prerequisites](#prerequisites)
    * [Install the JDK](#install-the-jdk)
  * [Build and Run](#build-and-run)
<!-- TOC -->

# CockroachDB multi-region demo

A collection of SQL scripts for demonstrating CockroachDB's 
multi-region capabilities, including:

- Data domiciling via super regions
- Regional-by-row and global table localities
- SQL queries for inspection of replica placement

The demo is wired to use AWS with the following regions:

- eu-central-1 (primary)
- us-east-1
- ap-northeast-1

It uses [roachprod](https://github.com/cockroachdb/cockroach/tree/master/pkg/cmd/roachprod) 
to provision AWS instances and deploy a cluster of nodes. 
See [deploy-aws.sh](deploy-aws.sh) for details. 

| Region         | Zone            | Nodes | LB/clients |
|----------------|-----------------|-------|:-----------|
| eu-central-1   | eu-central-1a   | 1     | 1          |
|                | eu-central-1b   | 1     |            |
|                | eu-central-1c   | 1     |            |
| us-east-1      | us-east-1a      | 1     | 1          |
|                | us-east-1b      | 1     |            |
|                | us-east-1c      | 1     |            |
| ap-northeast-1 | ap-northeast-1a | 1     | 1          |
|                | ap-northeast-1c | 1     |            |
|                | ap-northeast-1d | 1     |            |

If you prefer to use any other region/zone, make sure to search+replace all occurrences
in the SQL scripts.

## How to run the demo

Run the SQL scripts in the following order:

- [01-setup-cluster.sql](sql/01-setup-cluster.sql)
- [02-setup-schema.sql](sql/02-setup-schema.sql)
- [03-setup-data.sql](sql/03-setup-data.sql)
- [04-setup-transfer-fn.sql](sql/04-setup-transfer-fn.sql)

Here its time to inspect the replica placements:

- [demo-inspect.sql](sql/demo-inspect.sql)

This will add the super regions and constrain RF to factor 3:

- [05-setup-partitions.sql](sql/05-setup-partitions.sql)

Time to inspect the replica placements again:

- [demo-inspect.sql](sql/demo-inspect.sql)

Perhaps also show the mind blowing part of local query latencies:

[demo-latencies.sql](sql/demo-latencies.sql)

Lastly, clean up to repeat the demo if need be:

- [06-cleanup.sql](sql/06-cleanup.sql)

## Remarks

Super regions is a mechanism for combining data domiciling with region 
survival. Only zone survival can use placement restrictions. To demonstrate 
region survival with super region data domiciling, you will need at least 
three regions per super region. 

The reason is that each super region needs at least 3 subregions to survive 
the loss of one region, and each region should have 3 nodes for resilience
and to honor placement constraints. If there’s not enough nodes in each region 
to satisfy the survival goal (replication factor of 3), then it will prioritize 
survival and place replicas outside the defined constraints, which would be a 
violation of placement constraints. In that case, you could end up with 
replicas for eu in us or ap regions or any combination there of.

So: 3 super regions × 3 regions × 3 nodes = 27 nodes.

| Super region | Region         | Zone            | Nodes | LB/clients |
|:-------------|----------------|-----------------|-------|:-----------|
| eu           | eu-central-1   | eu-central-1a   | 1     | 1          |
|              |                | eu-central-1b   | 1     |            |
|              |                | eu-central-1c   | 1     |            |
|              | eu-west-1      | eu-west-1a      | 1     | 1          |
|              |                | eu-west-1b      | 1     |            |
|              |                | eu-west-1c      | 1     |            |
|              | eu-west-2      | eu-west-1a      | 1     | 1          |
|              |                | eu-west-1b      | 1     |            |
|              |                | eu-west-1c      | 1     |            |
| us           | us-east-1      | us-east-1a      | 1     | 1          |
|              |                | us-east-1b      | 1     |            |
|              |                | us-east-1c      | 1     |            |
|              | us-east-2      | us-east-2a      | 1     | 1          |
|              |                | us-east-2b      | 1     |            |
|              |                | us-east-2c      | 1     |            |
|              | us-west-1      | us-west-1a      | 1     | 1          |
|              |                | us-west-1b      | 1     |            |
|              |                | us-west-1c      | 1     |            |
| ap           | ap-northeast-1 | ap-northeast-1a | 1     | 1          |
|              |                | ap-northeast-1c | 1     |            |
|              |                | ap-northeast-1d | 1     |            |
|              | ap-northeast-2 | ap-northeast-2a | 1     | 1          |
|              |                | ap-northeast-2b | 1     |            |
|              |                | ap-northeast-2c | 1     |            |
|              | ap-southeast-1 | ap-southeast-1a | 1     | 1          |
|              |                | ap-southeast-1b | 1     |            |
|              |                | ap-southeast-1c | 1     |            |
| Σ            |                |                 | 27    | 9          |

Technically, you can also use only 2 super regions in which case its: 
2 super regions × 3 regions × 3 nodes = 18 nodes.

| Super region | Region         | Zone            | Nodes | LB/clients |
|:-------------|----------------|-----------------|-------|:-----------|
| eu           | eu-central-1   | eu-central-1a   | 1     | 1          |
|              |                | eu-central-1b   | 1     |            |
|              |                | eu-central-1c   | 1     |            |
|              | eu-west-1      | eu-west-1a      | 1     | 1          |
|              |                | eu-west-1b      | 1     |            |
|              |                | eu-west-1c      | 1     |            |
|              | eu-west-2      | eu-west-1a      | 1     | 1          |
|              |                | eu-west-1b      | 1     |            |
|              |                | eu-west-1c      | 1     |            |
| us           | us-east-1      | us-east-1a      | 1     | 1          |
|              |                | us-east-1b      | 1     |            |
|              |                | us-east-1c      | 1     |            |
|              | us-east-2      | us-east-2a      | 1     | 1          |
|              |                | us-east-2b      | 1     |            |
|              |                | us-east-2c      | 1     |            |
|              | us-west-1      | us-west-1a      | 1     | 1          |
|              |                | us-west-1b      | 1     |            |
|              |                | us-west-1c      | 1     |            |
| Σ            |                |                 | 18    | 6          |

# Demo app

The demo app can be used to run different queries against the cluster at scale.

## Prerequisites

- JDK 21 (or later)
    - https://openjdk.org/projects/jdk/21/
    - https://www.oracle.com/java/technologies/downloads/#java21
- CockroachDB 25.2 (or later)
    - https://www.cockroachlabs.com/docs/releases/

### Install the JDK

Ubuntu:

```shell
sudo apt-get install openjdk-21-jdk
```

MacOS (using sdkman):

```shell
curl -s "https://get.sdkman.io" | bash
sdk list java
sdk install java 21.. (pick version)  
```

## Build and Run

```shell
./mvnw clean install
./run.sh
```

This will print all CLI options and then exit.

---