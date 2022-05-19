package me.thunderbiscuit.kldk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import me.thunderbiscuit.kldk.network.connectPeer
import me.thunderbiscuit.kldk.network.createChannel
import me.thunderbiscuit.kldk.network.getLatestBlockHash
import me.thunderbiscuit.kldk.network.getLatestBlockHeight
import me.thunderbiscuit.kldk.utils.Config
import me.thunderbiscuit.kldk.utils.listPeers
import me.thunderbiscuit.kldk.utils.toByteArray
import org.ldk.structs.Result__u832APIErrorZ
import kotlin.system.exitProcess

fun main() {
    println("Hello, ${Config.nodeName}!\n")

    while (true) {
        print("Kldk ❯❯❯ ")
        val input: List<String> = readln().split(" ")

        try {
            Kldk()
                .subcommands(
                    StartNode(),
                    ConnectToPeer(),
                    ListPeers(),
                    GetBlockInfo(),
                    OpenChannel(),
                    Shutdown(),
                    Exit()
                )
                .parse(input)
            println()
        } catch (e: PrintHelpMessage) {
            echo(e.command.getFormattedHelp())
            println()
        } catch (e: UsageError) {
            echo(e.helpMessage())
            println()
        } catch (e: Throwable) {
            println("ERROR: ${e.printStackTrace()}")
            println()
        }
    }
}

class Kldk : CliktCommand() {
    override val commandHelp = """
        Kldk is a lightning node ⚡.

        Use any of the commands to interact with it.
    """

    override fun run() = Unit
}

class StartNode : CliktCommand(help = "Start your Kldk node", name = "startnode") {
    override fun run() {
        echo("Kldk starting...")
        startNode()
        echo("Up and running!")
    }
}

class ConnectToPeer : CliktCommand(name = "connectpeer", help = "Connect to a peer") {
    private val pubkey by option("--pubkey", help="Peer public key (required)").required()
    private val ip by option("--ip", help="Peer ip address (required)").required()
    private val port by option("--port", help="Peer port (required)").int().required()

    override fun run() {
        echo("Kldk attempting to connect to peer $pubkey@$ip:$port...")
        connectPeer(
            pubkey = pubkey,
            hostname = ip,
            port = port
        )
    }
}

class ListPeers : CliktCommand(name = "listpeers", help = "Print a list of connected peers") {
    override fun run() {
        val peers: List<String> = listPeers()
        when {
            peers.isEmpty() -> echo("No connected peers at the moment")
            else -> peers.forEachIndexed { index, peerPubkey ->
                echo("Peer ${index + 1}: $peerPubkey")
            }
        }
    }
}

class OpenChannel : CliktCommand(name = "openchannel", help = "Open a channel to a peer") {
    private val pubkey by option("--pubkey", help="Peer public key (required)").required()
    private val channelValue by option("--channelvalue", help="Channel value in millisatoshi (required)").required()
    private val pushAmount by option("--pushamount", help="The amount to push to the counterparty as part of the open, in millisatoshi").required()

    override fun run() {
        val response = createChannel(pubkey.toByteArray(), channelValue.toLong(), pushAmount.toLong(), 4242)
        when (response) {
            is Result__u832APIErrorZ.Result__u832APIErrorZ_OK -> println(response.res)
            is Result__u832APIErrorZ.Result__u832APIErrorZ_Err -> println(response.err.toString())
        }
    }
}

class GetBlockInfo : CliktCommand(name = "getblockinfo", help = "Print latest block height and hash") {
    override fun run() {
        runBlocking {
            val latestBlockHash = getLatestBlockHash()
            val latestBlockHeight = getLatestBlockHeight()
            echo("Latest block hash is $latestBlockHash")
            echo("Latest block height is $latestBlockHeight")
        }
    }
}

class Shutdown : CliktCommand(name = "shutdown", help = "Shutdown node") {
    override fun run() {
        echo("Shutting down node...")
    }
}

class Exit : CliktCommand(name = "exit", help = "Exit REPL") {
    override fun run() {
        echo("Exiting the REPL...")
        exitProcess(0)
    }
}
