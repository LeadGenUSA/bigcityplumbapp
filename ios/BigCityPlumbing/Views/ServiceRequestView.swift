import SwiftUI
import UIKit

private let SERVICE_TYPES: [String] = [
    "Emergency leak / burst pipe",
    "Clogged drain or toilet",
    "Water heater problem",
    "Heating system service",
    "Bathroom or kitchen remodel",
    "Inspection / quote",
    "Other",
]

struct ServiceRequestView: View {
    @Environment(\.openURL) private var openURL

    @State private var name = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var address = ""
    @State private var serviceType: String = SERVICE_TYPES.first!
    @State private var details = ""

    @State private var submitting = false
    @State private var alertMessage: String?
    @State private var showSuccess = false
    @State private var successMessage = ""

    var body: some View {
        Form {
            Section {
                Button {
                    if let url = AppConfig.telURL { openURL(url) }
                } label: {
                    HStack {
                        Image(systemName: "phone.fill")
                        Text("Call now: \(AppConfig.phoneNumberDisplay)").bold()
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .foregroundStyle(.white)
                    .background(Theme.brandOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            } header: {
                Text("Emergency? Call us first.")
            }

            Section("Your information") {
                TextField("Name", text: $name)
                    .textContentType(.name)
                TextField("Phone", text: $phone)
                    .keyboardType(.phonePad)
                    .textContentType(.telephoneNumber)
                TextField("Email", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                TextField("Service address", text: $address, axis: .vertical)
                    .textContentType(.fullStreetAddress)
                    .lineLimit(2...4)
            }

            Section("Type of service") {
                Picker("Type", selection: $serviceType) {
                    ForEach(SERVICE_TYPES, id: \.self) { Text($0) }
                }
                .pickerStyle(.menu)
            }

            Section("Describe the problem") {
                TextField("Details", text: $details, axis: .vertical)
                    .lineLimit(4...8)
            }

            Section {
                Button(action: submit) {
                    HStack {
                        if submitting {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "paperplane.fill")
                            Text("Submit Request").bold()
                        }
                    }
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .foregroundStyle(.white)
                    .background(Theme.brandBlue)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .disabled(submitting)
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("Request Service")
        .alert("Missing info", isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button("OK") { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .alert("Request sent", isPresented: $showSuccess) {
            Button("OK") { showSuccess = false }
        } message: {
            Text(successMessage)
        }
    }

    private func submit() {
        if name.isEmpty {
            alertMessage = "Please enter your name."; return
        }
        if phone.isEmpty {
            alertMessage = "Please enter a phone number we can reach you at."; return
        }
        if email.isEmpty {
            alertMessage = "Please enter your email address."; return
        }
        if !email.contains("@") || !email.contains(".") {
            alertMessage = "Please enter a valid email address."; return
        }
        if address.isEmpty {
            alertMessage = "Please enter the service address."; return
        }

        if AppConfig.serviceFormConfigured {
            Task { await sendInApp() }
        } else {
            sendViaMail()   // fallback until the endpoint keys are configured
        }
    }

    /// POST the request to the Supabase edge function, which emails it server-side.
    @MainActor
    private func sendInApp() async {
        guard let url = URL(string: AppConfig.serviceRequestURL) else { sendViaMail(); return }
        submitting = true
        defer { submitting = false }

        let payload: [String: String] = [
            "name": name, "phone": phone, "email": email,
            "address": address, "serviceType": serviceType, "message": details,
            "secret": AppConfig.appFormSecret,
        ]
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(AppConfig.supabaseAnonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer \(AppConfig.supabaseAnonKey)", forHTTPHeaderField: "Authorization")
        req.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        do {
            let (_, response) = try await URLSession.shared.data(for: req)
            if let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) {
                successMessage = "Thanks! Your request was sent to Big City Plumbing — we'll be in touch shortly."
                clearForm()
                showSuccess = true
            } else {
                alertMessage = "Sorry, we couldn't send your request. Please call \(AppConfig.phoneNumberDisplay)."
            }
        } catch {
            alertMessage = "Network problem sending your request. Please call \(AppConfig.phoneNumberDisplay)."
        }
    }

    private func clearForm() {
        name = ""; phone = ""; email = ""; address = ""; details = ""
        serviceType = SERVICE_TYPES.first!
    }

    /// Fallback: open the customer's mail app pre-filled (used until the
    /// in-app endpoint is configured).
    private func sendViaMail() {
        let subject = "Service request: \(serviceType)"
        var bodyLines: [String] = ["Name: \(name)", "Phone: \(phone)"]
        if !email.isEmpty { bodyLines.append("Email: \(email)") }
        bodyLines.append(contentsOf: [
            "Address: \(address)", "Service: \(serviceType)", "",
            "Details:", details.isEmpty ? "(none)" : details,
        ])
        let body = bodyLines.joined(separator: "\n")

        let allowed = CharacterSet.urlQueryAllowed
        guard
            let subjEnc = subject.addingPercentEncoding(withAllowedCharacters: allowed),
            let bodyEnc = body.addingPercentEncoding(withAllowedCharacters: allowed),
            let url = URL(string: "mailto:\(AppConfig.email)?subject=\(subjEnc)&body=\(bodyEnc)")
        else {
            alertMessage = "Couldn't build the email. Please call \(AppConfig.phoneNumberDisplay)."
            return
        }

        UIApplication.shared.open(url) { ok in
            if ok {
                successMessage = "We've opened your email app. Send the message to finish your request."
                showSuccess = true
            } else {
                alertMessage = "No email app found. Please call \(AppConfig.phoneNumberDisplay)."
            }
        }
    }
}

#Preview {
    NavigationStack { ServiceRequestView() }
}
