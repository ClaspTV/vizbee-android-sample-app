package tv.vizbee.demo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.FragmentUserLoginBinding
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.model.LoginRequest
import tv.vizbee.demo.model.LoginResponse
import tv.vizbee.demo.network.ApiInterface
import tv.vizbee.demo.network.NetworkInstance
import tv.vizbee.demo.vizbee.VizbeeHomeSSOAdapter
import tv.vizbee.demo.vizbee.VizbeeWrapper
import tv.vizbee.utils.Logger

class UserLoginFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentUserLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Logger.d(LOG_TAG, "onCreateView called")
        binding = FragmentUserLoginBinding.inflate(inflater, container, false)
        binding.loginSubmit.setOnClickListener(this)
        return binding.root
    }

    override fun onClick(v: View) {
        if (v.id == R.id.login_submit) {
            login()
        }
    }

    private fun login() {
        val email = binding.loginUsername.text.toString()
        val password = binding.loginPassword.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            val apiInterface = NetworkInstance.getInstance().create(ApiInterface::class.java)
            val loginRequest = LoginRequest(
                email,
                password
            )

            val call = apiInterface.signIn(loginRequest)
            call.enqueue(
                object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            VizbeeWrapper.context?.get()?.let {
                                SharedPreferenceHelper.saveAuthToken(body.authToken)

                                // ---------------------------
                                // [BEGIN] Vizbee Integration
                                // ---------------------------
                                VizbeeHomeSSOAdapter().updateRegCodeStatus(body.authToken)
                                // ---------------------------
                                // [END] Vizbee Integration
                                // ---------------------------

                                activity?.invalidateOptionsMenu()
                                mFragmentController.popBackStack()
                                Toast.makeText(it, "Signin success", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Logger.e(LOG_TAG, "Signin Failure", t)
                        VizbeeWrapper.context?.get()?.let {
                            Toast.makeText(it, "Signin Failure", Toast.LENGTH_LONG).show()
                        }
                        mFragmentController.popBackStack()
                    }
                })
        } else {
            Logger.e(LOG_TAG, getString(R.string.invalid_credentials))
        }
    }

    companion object {
        fun newInstance(): UserLoginFragment {
            return UserLoginFragment()
        }
    }

}
