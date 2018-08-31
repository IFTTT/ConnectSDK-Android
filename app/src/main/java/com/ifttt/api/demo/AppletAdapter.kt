package com.ifttt.api.demo

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ifttt.IftttApiClient
import com.ifttt.api.PendingResult
import com.ifttt.api.demo.api.ApiHelper
import com.ifttt.Applet
import com.ifttt.ErrorResponse
import com.squareup.picasso.Picasso

/**
 * RecyclerView adapter for showing a list of Applets returned from the API.
 */
class AppletAdapter(private val applets: MutableList<Applet>,
        private val clickListenerFactory: AppletConfigurationClickListenerFactory) :
        RecyclerView.Adapter<AppletAdapter.AppletViewHolder>() {

    override fun onBindViewHolder(holder: AppletViewHolder, position: Int) {
        val applet = applets[position]
        val primaryService = applet.primaryService

        holder.titleView.text = applet.name
        holder.descriptionView.text = applet.description

        // Using primary service to brand the Applet.
        holder.root.setCardBackgroundColor(primaryService.brandColor)
        Picasso.with(holder.itemView.context).load(primaryService.colorIconUrl).into(holder.iconView)

        holder.turnOnButton.setOnClickListener(clickListenerFactory.newOnClickListener(applets, holder))

        holder.enableDisableButton.setOnClickListener {
            // For Applets that have been turned on before, users can the AppletConfigApi to enable or disable the
            // Applet without going through the activation flow again.
            val appletAtPosition = applets[holder.adapterPosition]
            val resultCallback = object : PendingResult.ResultCallback<Applet> {
                override fun onSuccess(result: Applet) {
                    applets[holder.adapterPosition] = result
                    if (result.status == Applet.Status.enabled) {
                        holder.enableDisableButton.setText(R.string.disable)
                    } else {
                        holder.enableDisableButton.setText(R.string.enable)
                    }
                }

                override fun onFailure(errorResponse: ErrorResponse) {
                    Toast.makeText(holder.itemView.context, errorResponse.message, Toast.LENGTH_LONG).show()
                }
            }

            val appletConfigApi = IftttApiClient.getInstance().appletConfigApi()
            if (appletAtPosition.status == Applet.Status.enabled) {
                appletConfigApi.disableApplet(ApiHelper.SERVICE_ID, appletAtPosition.id).execute(resultCallback)
            } else if (appletAtPosition.status == Applet.Status.disabled) {
                appletConfigApi.enableApplet(ApiHelper.SERVICE_ID, appletAtPosition.id).execute(resultCallback)
            }
        }

        if (applet.status == Applet.Status.unknown || applet.status == Applet.Status.never_enabled) {
            holder.enableDisableButton.visibility = View.GONE
            holder.turnOnButton.setText(R.string.turn_on)
        } else {
            holder.enableDisableButton.visibility = View.VISIBLE
            if (applet.status == Applet.Status.enabled) {
                holder.enableDisableButton.setText(R.string.disable)
            } else {
                holder.enableDisableButton.setText(R.string.enable)
            }
            holder.turnOnButton.setText(R.string.configure)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppletViewHolder {
        return AppletViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun getItemCount(): Int {
        return applets.size
    }

    class AppletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root = itemView as CardView
        val titleView = itemView.findViewById<TextView>(R.id.applet_title)!!
        val descriptionView = itemView.findViewById<TextView>(R.id.applet_description)!!
        val iconView = itemView.findViewById<ImageView>(R.id.icon)!!
        val turnOnButton = itemView.findViewById<Button>(R.id.turn_on_button)!!
        val enableDisableButton = itemView.findViewById<Button>(R.id.enable_disable_button)!!
    }
}
