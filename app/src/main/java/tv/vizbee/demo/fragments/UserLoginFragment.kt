package tv.vizbee.demo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.vizbee.demo.R
import tv.vizbee.demo.databinding.FragmentUserLoginBinding
import tv.vizbee.demo.helper.SharedPreferenceHelper
import tv.vizbee.demo.model.LoginRequest
import tv.vizbee.demo.model.LoginResponse
import tv.vizbee.demo.network.LoginApiInterface
import tv.vizbee.demo.network.NetworkInstance
import tv.vizbee.demo.vizbee.VizbeeHomeSSOAdapter
import tv.vizbee.utils.Logger

class UserLoginFragment : BaseFragment(), View.OnClickListener {

    private lateinit var binding: FragmentUserLoginBinding
    var isHomeSSOLogin = false

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

        isHomeSSOLogin = arguments?.getBoolean(ARG_HOME_SSO_LOGIN, false) ?: false

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
            val loginApiInterface =
                NetworkInstance.getInstance().create(LoginApiInterface::class.java)
            val loginRequest = LoginRequest(
                email,
                password
            )

            val call = loginApiInterface.signIn(loginRequest)
            call.enqueue(
                object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            SharedPreferenceHelper.saveAuthToken(body.authToken)

                            // ---------------------------
                            // [BEGIN] Vizbee Integration
                            // ---------------------------
                            if (isHomeSSOLogin) {
                                VizbeeHomeSSOAdapter().updateRegCodeStatus(body.authToken)
                            }
                            // ---------------------------
                            // [END] Vizbee Integration
                            // ---------------------------

                            activity?.invalidateOptionsMenu()
                            mFragmentController.popBackStack()
                            Toast.makeText(context, "Signin success", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Logger.e(LOG_TAG, "Signin Failure", t)
                        Toast.makeText(context, "Signin Failure", Toast.LENGTH_LONG).show()
                        mFragmentController.popBackStack()
                    }
                })
        } else {
            Logger.e(LOG_TAG, getString(R.string.invalid_credentials))
        }
    }

    companion object {
        private const val ARG_HOME_SSO_LOGIN = "HOME_SSO_LOGIN"

        fun newInstance(isHomeSSOLogin: Boolean) = UserLoginFragment().apply {
            arguments = bundleOf(ARG_HOME_SSO_LOGIN to isHomeSSOLogin)
        }
    }

}
