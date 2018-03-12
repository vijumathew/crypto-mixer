# Crypto Mixer

## Description

A cryptocurrency mixer

## Running

The main method takes arguments for the output addresses and an optional argument for the max wait time for a transfer. This is located in the `core` namespace.

### Leiningen

Download the repo and cd into it:

```bash
$ cd crypto-mixer
```

Now enter:

```bash
$ lein run -i INTERVAL_TIME -a ADDRESS_ONE -a ADDRESS_TWO -a ...
...
Send jobcoins to OUTPUT_ADDRESS - listening for 5 minutes...
```

Once you create a transfer to `OUTPUT_ADDRESS`, the mixer will detect it and send a randomized set of transactions to the provided input addresses.

## Packaging

It is recommended to use uberjar to create a standalone JAR.

```bash
$ lein uberjar
```

This can be executed as a standalone executable with the same CLI arguments.
```bash
$ java -jar target/crypto-mixer-0.1-standalone.jar -i ...
```

## API
The biggest pieces of the mixer lie in the `transactions` namespace.

* `get-new-addr-blocking` - finds an unused address that is used by the mixer to receive the initial transfer. Note this is blocking and should be used carefully.
* `split-transfer-to-addrs` - this is the most important function for the mixer. this waits on ADDR to get an initial transfer, then sends this amount to house-addr. Next, this schedules  randomized transactions with max-length INTERVAL to ADDRESSES using `schedule-transfers`.

## Roadmap
  * Support multiple cryptocurrencies
  * Improve randomization algorithm
  * Improve error reporting and retrying
  * Support dynamic house account
